#!/usr/bin/env python3
"""Exercise the socket and checkpoint recovery in a disposable repository."""

import json
import subprocess
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "coordinator"))
from agent_engine_local.transport.ipc import request


def start(home):
    process = subprocess.Popen(
        [
            sys.executable,
            str(ROOT / "plugin/scripts/bridge.py"),
            "--home",
            str(home),
            "serve",
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
    )
    for _ in range(100):
        if process.poll() is not None:
            details = process.stderr.read().decode()
            process.stderr.close()
            raise RuntimeError("Coordinator startup failed: " + details)
        if (home / "coordinator.sock").exists():
            try:
                request(
                    home,
                    {
                        "operation": "context",
                        "arguments": {"repository": str(home.parent)},
                    },
                    timeout=0.2,
                )
                return process
            except Exception:
                pass
        time.sleep(0.02)
    process.terminate()
    process.wait(timeout=5)
    raise RuntimeError("Coordinator startup timed out")


def stop(process):
    process.terminate()
    process.wait(timeout=5)
    process.stderr.close()


def demo():
    with tempfile.TemporaryDirectory(prefix="ae-") as directory:
        base = Path(directory).resolve()
        repo = base / "repository"
        repo.mkdir()
        (repo / "message.txt").write_text("Before\n")
        home = base / "state"
        daemon = start(home)
        states = []

        def call(operation, **arguments):
            result = request(home, {"operation": operation, "arguments": arguments})
            states.append(result.get("state", "CONTEXT_READ"))
            return result

        try:
            task = call(
                "create",
                repository=str(repo),
                scope=["message.txt"],
                plan="Change message.txt to After and verify its contents",
                rules=["Preserve pre-existing work; no commits"],
                tests=[
                    [
                        sys.executable,
                        "-c",
                        "from pathlib import Path; assert Path('message.txt').read_text() == 'After\\n'",
                    ]
                ],
            )["id"]
            call("prepare", task_id=task)
            call("context", repository=str(repo))
            call(
                "apply",
                task_id=task,
                operation_id="demo-edit-1",
                path="message.txt",
                content="After\n",
            )
            # Abrupt daemon termination demonstrates durable checkpoint and stale-socket recovery.
            stop(daemon)
            daemon = start(home)
            call("resume", task_id=task)
            call("verify", task_id=task)
            call("review", task_id=task, findings=[])
            final = call("release", task_id=task)
            assert (repo / "message.txt").read_text() == "After\n"
            assert final["state"] == "LOCAL_DONE_PENDING_SYNC"
            return {
                "result": "PASS",
                "transport": "Unix domain socket",
                "states": states,
                "limits": "Disposable local Quick task; no Codex lifecycle/OAuth certification",
            }
        finally:
            if daemon.poll() is None:
                stop(daemon)


if __name__ == "__main__":
    print(json.dumps(demo(), indent=2))
