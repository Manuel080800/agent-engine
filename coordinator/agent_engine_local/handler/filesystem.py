"""Conservative regular-file operations on existing, non-symlink directories.

Locks are cooperative. Checks and atomic replacement do not provide filesystem CAS
against external editors. Unsupported metadata is rejected before backup or editing.
"""

import hashlib
import base64
import ctypes
import errno
import os
import stat
import sys
import uuid
from pathlib import Path, PurePosixPath

from agent_engine_local.common.errors import Conflict, EngineError


def digest(data):
    return hashlib.sha256(data).hexdigest()


def metadata(fd, info):
    if getattr(info, "st_flags", 0):
        raise Conflict("Filesystem flags are unsupported")
    if sys.platform == "darwin":
        native = ctypes.CDLL(None, use_errno=True)
        native.flistxattr.argtypes = [
            ctypes.c_int,
            ctypes.c_void_p,
            ctypes.c_size_t,
            ctypes.c_int,
        ]
        native.flistxattr.restype = ctypes.c_ssize_t
        attributes = native.flistxattr(fd, None, 0, 0)
        if attributes < 0:
            raise Conflict("Cannot verify extended attributes")
        native.acl_get_fd_np.argtypes = [ctypes.c_int, ctypes.c_int]
        native.acl_get_fd_np.restype = ctypes.c_void_p
        native.acl_free.argtypes = [ctypes.c_void_p]
        ctypes.set_errno(0)
        acl = native.acl_get_fd_np(fd, 0x100)  # ACL_TYPE_EXTENDED from macOS SDK.
        if acl:
            native.acl_free(acl)
            raise Conflict("ACL metadata requires a different backup strategy")
        if ctypes.get_errno() != errno.ENOENT:
            raise Conflict("Cannot verify ACL metadata")
        buffer = ctypes.create_string_buffer(attributes)
        if native.flistxattr(fd, buffer, attributes, 0) != attributes:
            raise Conflict("Extended attributes changed while reading")
        names = [name.decode() for name in buffer.raw.split(b"\x00") if name]
        if any(name != "com.apple.provenance" for name in names):
            raise Conflict("Unsupported extended attribute")
        result = {}
        native.fgetxattr.argtypes = [
            ctypes.c_int,
            ctypes.c_char_p,
            ctypes.c_void_p,
            ctypes.c_size_t,
            ctypes.c_uint32,
            ctypes.c_int,
        ]
        native.fgetxattr.restype = ctypes.c_ssize_t
        for name in names:
            size = native.fgetxattr(fd, name.encode(), None, 0, 0, 0)
            if not 0 <= size <= 65536:
                raise Conflict("Unsupported extended attribute size")
            value = ctypes.create_string_buffer(size)
            if native.fgetxattr(fd, name.encode(), value, size, 0, 0) != size:
                raise Conflict("Extended attribute changed while reading")
            result[name] = base64.b64encode(value.raw).decode()
        return result
    if not hasattr(os, "listxattr"):
        raise Conflict("Metadata verification is unsupported on this platform")
    if os.listxattr(fd):
        raise Conflict("Unsupported extended attributes")
    return {}


def set_metadata(fd, attributes):
    if sys.platform != "darwin":
        return
    native = ctypes.CDLL(None, use_errno=True)
    native.fsetxattr.argtypes = [
        ctypes.c_int,
        ctypes.c_char_p,
        ctypes.c_void_p,
        ctypes.c_size_t,
        ctypes.c_uint32,
        ctypes.c_int,
    ]
    native.fremovexattr.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int]
    for name in metadata(fd, os.fstat(fd)):
        if name not in attributes and native.fremovexattr(fd, name.encode(), 0) != 0:
            raise Conflict("Cannot remove temporary attribute")
    for name, encoded in attributes.items():
        value = base64.b64decode(encoded)
        if native.fsetxattr(fd, name.encode(), value, len(value), 0, 0) != 0:
            raise Conflict("Cannot preserve extended attribute")


def relative_path(value):
    if not isinstance(value, str) or not value or "\x00" in value:
        raise EngineError("Invalid relative path")
    path = PurePosixPath(value)
    if path.is_absolute() or any(p in ("..", ".git") for p in path.parts):
        raise EngineError("Path outside permitted scope")
    if path.as_posix() != value or value == ".":
        raise EngineError("Path must be normalized")
    return value


class Repository:
    def __init__(self, root):
        self.root = Path(root).resolve(strict=True)
        if not self.root.is_dir():
            raise EngineError("Repository root must be a directory")
        info = self.root.stat()
        self.identity = (info.st_dev, info.st_ino)
        self.key = digest(f"{self.root}:{info.st_dev}:{info.st_ino}".encode())

    def parent(self, name):
        relative_path(name)
        info = self.root.stat()
        if (info.st_dev, info.st_ino) != self.identity:
            raise Conflict("Repository root identity changed")
        fd = os.open(self.root, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
        try:
            for part in PurePosixPath(name).parts[:-1]:
                child = os.open(
                    part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd
                )
                os.close(fd)
                fd = child
            return fd, PurePosixPath(name).name
        except OSError as exc:
            os.close(fd)
            raise Conflict("Parent directory missing or unsafe") from exc

    def read(self, name, max_bytes=16 * 1024 * 1024):
        parent, leaf = self.parent(name)
        try:
            try:
                fd = os.open(
                    leaf, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK, dir_fd=parent
                )
            except FileNotFoundError:
                return {"kind": "missing"}, None
            except OSError as exc:
                raise Conflict("File cannot be opened safely") from exc
            with os.fdopen(fd, "rb") as stream:
                info = os.fstat(stream.fileno())
                if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1:
                    raise Conflict("Only regular files without hardlinks are supported")
                if info.st_size > max_bytes:
                    raise EngineError("File exceeds backup limit")
                attributes = metadata(stream.fileno(), info)
                if stat.S_IMODE(info.st_mode) & 0o7000:
                    raise Conflict("Special permissions are unsupported")
                data = stream.read(max_bytes + 1)
                after = os.fstat(stream.fileno())
                if len(data) > max_bytes or (
                    info.st_size,
                    info.st_mtime_ns,
                    info.st_ctime_ns,
                ) != (after.st_size, after.st_mtime_ns, after.st_ctime_ns):
                    raise Conflict("File changed while preparing snapshot")
                current = os.stat(leaf, dir_fd=parent, follow_symlinks=False)
                if (current.st_dev, current.st_ino) != (info.st_dev, info.st_ino):
                    raise Conflict("File identity changed while reading")
                return {
                    "kind": "file",
                    "hash": digest(data),
                    "mode": stat.S_IMODE(info.st_mode),
                    "attributes": attributes,
                    "device": info.st_dev,
                    "inode": info.st_ino,
                    "mtime_ns": info.st_mtime_ns,
                    "ctime_ns": info.st_ctime_ns,
                }, data
        finally:
            os.close(parent)

    def snapshot(self):
        result = {}
        for parent, directories, files in os.walk(self.root, followlinks=False):
            directories[:] = sorted(d for d in directories if d != ".git")
            for entry in directories[:] + sorted(f for f in files if f != ".git"):
                path = Path(parent) / entry
                name = path.relative_to(self.root).as_posix()
                info = path.lstat()
                if stat.S_ISLNK(info.st_mode):
                    result[name] = {
                        "kind": "symlink",
                        "target_hash": digest(os.readlink(path).encode()),
                    }
                    if entry in directories:
                        directories.remove(entry)
                elif stat.S_ISDIR(info.st_mode):
                    result[name] = {
                        "kind": "directory",
                        "device": info.st_dev,
                        "inode": info.st_ino,
                    }
                else:
                    result[name] = self.read(name)[0]
        return result

    def replace(self, name, expected, data, mode=0o644, attributes=None):
        if self.read(name)[0] != expected:
            raise Conflict("File diverged from last managed state")
        parent, leaf = self.parent(name)
        temporary = ".agent-engine-" + uuid.uuid4().hex
        try:
            if data is None:
                if expected["kind"] != "missing":
                    os.unlink(leaf, dir_fd=parent)
            else:
                fd = os.open(
                    temporary,
                    os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW,
                    0o600,
                    dir_fd=parent,
                )
                with os.fdopen(fd, "wb") as stream:
                    stream.write(data)
                    os.fchmod(stream.fileno(), mode)
                    set_metadata(
                        stream.fileno(),
                        (
                            attributes
                            if attributes is not None
                            else expected.get("attributes", {})
                        ),
                    )
                    stream.flush()
                    os.fsync(stream.fileno())
                # Recheck immediately before replacement; external TOCTOU remains possible.
                if self.read(name)[0] != expected:
                    raise Conflict("File changed before atomic replacement")
                os.replace(temporary, leaf, src_dir_fd=parent, dst_dir_fd=parent)
            os.fsync(parent)
        finally:
            try:
                os.unlink(temporary, dir_fd=parent)
            except FileNotFoundError:
                pass
            os.close(parent)
        return self.read(name)[0]
