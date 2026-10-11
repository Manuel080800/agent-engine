import os
import stat
from pathlib import Path
from agent_engine_local.common.errors import EngineError


def private_directory(path):
    path = Path(path).absolute()
    path.mkdir(parents=True, mode=0o700, exist_ok=True)
    # macOS /var is a system alias. Canonicalize ancestors, never the final entry.
    path = path.parent.resolve(strict=True) / path.name
    info = path.lstat()
    if (
        not stat.S_ISDIR(info.st_mode)
        or info.st_uid != os.getuid()
        or path.resolve() != path
    ):
        raise EngineError(
            "State directory must be owned by this user and contain no symlink"
        )
    os.chmod(path, 0o700)
    return path
