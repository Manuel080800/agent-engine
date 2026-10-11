import base64
import importlib.util
import json
import os
import subprocess
import sys
import tempfile
import threading
import unittest
from pathlib import Path

from agent_engine_local.transport.ipc import Server, request
from agent_engine_local.common.errors import EngineError

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location(
    "guard", ROOT / "plugin/hooks/scripts/guard.py"
)
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class IntegrationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="ae-")
        self.base = Path(self.temp.name).resolve()
        self.repo = self.base / "repo"
        self.repo.mkdir()
        (self.repo / "file.txt").write_text("before")
        self.home = self.base / "state"
        self.server = Server(self.home)
        self.thread = threading.Thread(target=self.server.serve_forever)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.thread.join(timeout=5)
        self.server.server_close()
        self.temp.cleanup()

    def call(self, operation, **arguments):
        return request(self.home, {"operation": operation, "arguments": arguments})

    def test_ipc_rejects_unregistered_operations(self):
        for operation in ("COMPLETED", "approve", "consume_sol", "close", "store"):
            with self.assertRaises(EngineError):
                self.call(operation)

    def test_ipc_runs_task_and_returns_reduced_response(self):
        task = self.call(
            "create",
            repository=str(self.repo),
            scope=["file.txt"],
            plan="change file",
            rules=["no commits"],
            tests=[[sys.executable, "-c", "raise SystemExit(0)"]],
        )
        self.assertNotIn("backups", task)
        self.assertNotIn("expected", task)
        self.call("prepare", task_id=task["id"])
        self.call("apply", task_id=task["id"], path="file.txt", content="after")
        self.call("verify", task_id=task["id"])
        self.call("review", task_id=task["id"], findings=[])
        self.assertEqual(
            self.call("release", task_id=task["id"])["state"], "LOCAL_DONE_PENDING_SYNC"
        )

    def test_socket_permissions_and_duplicate_daemon(self):
        self.assertEqual((self.home / "coordinator.sock").stat().st_mode & 0o777, 0o600)
        with self.assertRaises((EngineError, BlockingIOError)):
            Server(self.home)
        self.assertFalse(self.call("context", repository=str(self.repo))["active"])

    def test_mcp_only_exposes_read_only_context(self):
        messages = [
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "initialize",
                "params": {"protocolVersion": "2025-11-25"},
            },
            {"jsonrpc": "2.0", "method": "notifications/initialized"},
            {"jsonrpc": "2.0", "id": 2, "method": "tools/list"},
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {"name": "get_context", "arguments": {}},
            },
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {"name": "complete", "arguments": {}},
            },
        ]
        result = subprocess.run(
            [
                sys.executable,
                str(ROOT / "plugin/scripts/bridge.py"),
                "--home",
                str(self.home),
                "mcp",
            ],
            cwd=self.repo,
            input="".join(json.dumps(m) + "\n" for m in messages),
            capture_output=True,
            text=True,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        responses = [json.loads(line) for line in result.stdout.splitlines()]
        self.assertEqual(len(responses), 4)
        self.assertEqual(responses[0]["result"]["protocolVersion"], "2025-11-25")
        self.assertEqual(
            [t["name"] for t in responses[1]["result"]["tools"]], ["get_context"]
        )
        self.assertFalse(
            json.loads(responses[2]["result"]["content"][0]["text"])["active"]
        )
        self.assertEqual(responses[3]["error"]["code"], -32602)

    def test_hook_reads_context_and_blocks_direct_edits(self):
        task = self.call(
            "create",
            repository=str(self.repo),
            scope=["file.txt"],
            plan="plan",
            rules=["rule"],
            tests=[["true"]],
        )
        event = {
            "hook_event_name": "PreToolUse",
            "cwd": str(self.repo),
            "tool_name": "apply_patch",
            "tool_input": {"command": "edit"},
        }
        environment = dict(os.environ, AGENT_ENGINE_HOME=str(self.home))
        result = subprocess.run(
            [sys.executable, str(ROOT / "plugin/hooks/scripts/guard.py")],
            input=json.dumps(event),
            capture_output=True,
            text=True,
            env=environment,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(
            json.loads(result.stdout)["hookSpecificOutput"]["permissionDecision"],
            "deny",
        )
        event["hook_event_name"] = "SessionStart"
        result = subprocess.run(
            [sys.executable, str(ROOT / "plugin/hooks/scripts/guard.py")],
            input=json.dumps(event),
            capture_output=True,
            text=True,
            env=environment,
        )
        context = json.loads(
            json.loads(result.stdout)["hookSpecificOutput"]["additionalContext"]
        )
        self.assertEqual(context["task_id"], task["id"])

    def test_guard_rejects_shell_composition_and_expansion(self):
        message = {
            "operation": "apply",
            "arguments": {"task_id": "example", "path": "file.txt", "content": "new"},
        }
        encoded = base64.b64encode(json.dumps(message).encode()).decode()
        command = (
            f"python3 '{ROOT}/plugin/scripts/bridge.py' request --base64 {encoded}"
        )
        self.assertTrue(guard.managed_call(command))
        for suffix in (
            "; touch escaped",
            " && true",
            " > escaped",
            " $(touch escaped)",
            "\ntrue",
        ):
            self.assertFalse(guard.managed_call(command + suffix))
        self.assertFalse(
            guard.managed_call(command.replace("bridge.py", "untrusted.py"))
        )

    def test_guard_failure_closes_direct_tool_path(self):
        event = {
            "hook_event_name": "PreToolUse",
            "tool_name": "Bash",
            "tool_input": {"command": "touch file.txt"},
        }
        result = guard.decision(event, {"unavailable": True})
        self.assertEqual(result["hookSpecificOutput"]["permissionDecision"], "deny")


if __name__ == "__main__":
    unittest.main()
