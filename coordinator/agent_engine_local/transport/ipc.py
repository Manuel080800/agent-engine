import json
import os
import socket
import socketserver
import stat
from pathlib import Path

from agent_engine_local.service.coordinator import Coordinator
from agent_engine_local.common.errors import EngineError

MAX_MESSAGE = 2 * 1024 * 1024
OPERATIONS = {
    "create",
    "prepare",
    "apply",
    "verify",
    "retry",
    "review",
    "restore",
    "resume",
    "release",
    "context",
    "import_remote",
    "synchronize",
    "next_block",
    "collect_backups",
    "run_role",
}


def default_home():
    return Path(os.environ.get("AGENT_ENGINE_HOME", Path.home() / ".agent-engine"))


def request(home, message, timeout=150):
    path = Path(home) / "coordinator.sock"
    info = path.lstat()
    if (
        not stat.S_ISSOCK(info.st_mode)
        or info.st_uid != os.getuid()
        or stat.S_IMODE(info.st_mode) != 0o600
    ):
        raise EngineError("Unsafe coordinator socket")
    data = json.dumps(message).encode() + b"\n"
    if len(data) > MAX_MESSAGE:
        raise EngineError("Request too large")
    with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as client:
        client.settimeout(timeout)
        client.connect(str(path))
        client.sendall(data)
        with client.makefile("rb") as stream:
            response = stream.readline(MAX_MESSAGE + 1)
    if len(response) > MAX_MESSAGE:
        raise EngineError("Response too large")
    result = json.loads(response)
    if not result["ok"]:
        raise EngineError(result["error"])
    return result["result"]


class Handler(socketserver.StreamRequestHandler):
    def handle(self):
        self.connection.settimeout(10)
        try:
            if hasattr(self.connection, "getpeereid"):
                uid, _ = self.connection.getpeereid()
                if uid != os.getuid():
                    raise EngineError("Peer identity mismatch")
            raw = self.rfile.readline(MAX_MESSAGE + 1)
            if len(raw) > MAX_MESSAGE or not raw.endswith(b"\n"):
                raise EngineError("Invalid request framing")
            message = json.loads(raw)
            if not isinstance(message, dict) or set(message) != {
                "operation",
                "arguments",
            }:
                raise EngineError("Invalid request envelope")
            if message["operation"] not in OPERATIONS or not isinstance(
                message["arguments"], dict
            ):
                raise EngineError("Unsupported operation")
            result = getattr(self.server.coordinator, message["operation"])(
                **message["arguments"]
            )
            # Operational journal/manifests stay local; IPC callers get a reduced response.
            if isinstance(result, dict) and "id" in result:
                result = {
                    k: result[k]
                    for k in (
                        "id",
                        "state",
                        "revision",
                        "scope",
                        "plan_hash",
                        "evidence",
                        "review",
                        "expires_at",
                    )
                }
            response = {"ok": True, "result": result}
        except EngineError as exc:
            response = {"ok": False, "error": str(exc)}
        except (ValueError, TypeError, KeyError):
            response = {"ok": False, "error": "Invalid typed request"}
        except Exception:
            response = {
                "ok": False,
                "error": "Operational failure; inspect local task state before retrying",
            }
        self.wfile.write(json.dumps(response).encode() + b"\n")


class Server(socketserver.ThreadingUnixStreamServer):
    daemon_threads = False
    block_on_close = True

    def __init__(self, home):
        self.coordinator = Coordinator(home)
        self.path = self.coordinator.store.home / "coordinator.sock"
        # A separate lifetime lock protects stale-socket cleanup from concurrent startups.
        import fcntl

        self.daemon_lock = os.open(
            self.coordinator.store.home / "daemon.lock",
            os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW,
            0o600,
        )
        try:
            fcntl.flock(self.daemon_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            if self.path.exists():
                if not stat.S_ISSOCK(self.path.lstat().st_mode):
                    raise EngineError("Socket path occupied by a non-socket")
                self.path.unlink()
            super().__init__(str(self.path), Handler)
            os.chmod(self.path, 0o600)
        except Exception:
            self.coordinator.close()
            os.close(self.daemon_lock)
            raise

    def server_close(self):
        super().server_close()
        self.coordinator.close()
        self.path.unlink(missing_ok=True)
        os.close(self.daemon_lock)
