"""Synchronous, conservative managed execution hook; it never parses shell commands as safe edits."""

import base64
import json
import re
import shlex
import sys
from pathlib import Path

PLUGIN_ROOT = Path(__file__).resolve().parents[2]
runtime = PLUGIN_ROOT / "runtime"
sys.path.insert(
    0, str(runtime if runtime.is_dir() else PLUGIN_ROOT.parent / "coordinator")
)

from agent_engine_local.transport.ipc import default_home, request


def managed_call(command):
    if not isinstance(command, str) or any(c in command for c in "\n\r$`;|&<>*?()"):
        return False
    try:
        argv = shlex.split(command)
        if len(argv) != 5 or argv[0] not in {"python3", sys.executable}:
            return False
        if Path(argv[1]).absolute() != PLUGIN_ROOT / "scripts" / "bridge.py" or argv[
            2:4
        ] != ["request", "--base64"]:
            return False
        if not re.fullmatch(r"[A-Za-z0-9+/=]+", argv[4]):
            return False
        message = json.loads(base64.b64decode(argv[4], validate=True))
        return (
            isinstance(message, dict)
            and set(message) == {"operation", "arguments"}
            and message["operation"]
            in {
                "context",
                "prepare",
                "apply",
                "verify",
                "review",
                "retry",
                "restore",
                "resume",
                "release",
                "synchronize",
                "next_block",
                "run_role",
            }
            and isinstance(message["arguments"], dict)
        )
    except (ValueError, TypeError):
        return False


def decision(event, context):
    name = event.get("hook_event_name")
    if name == "PreToolUse":
        tool = event.get("tool_name", "")
        arguments = event.get("tool_input", {})
        if tool in {"Bash", "exec_command"} and managed_call(arguments.get("command")):
            return {}
        if not context.get("active") and not context.get("unavailable"):
            return {}
        if tool.endswith("__get_context") and "agent_engine" in tool.replace("-", "_"):
            return {}
        # No distinction based solely on model-provided "readOnly" claims.
        return {
            "hookSpecificOutput": {
                "hookEventName": name,
                "permissionDecision": "deny",
                "permissionDecisionReason": "Agent Engine managed task: use the coordinator bridge; direct tools are not certified for this block.",
            }
        }
    if name in {"SessionStart", "PostCompact"}:
        text = json.dumps(context, ensure_ascii=False)
        return {
            "hookSpecificOutput": {"hookEventName": name, "additionalContext": text}
        }
    if name == "PreCompact":
        return {
            "systemMessage": "Local checkpoint is durable. Resume the recorded task; uncertain operations must not be replayed."
        }
    return {}


def main():
    try:
        event = json.loads(sys.stdin.buffer.read(2 * 1024 * 1024 + 1))
        if not isinstance(event, dict):
            raise ValueError("Invalid event")
        try:
            context = request(
                default_home(),
                {"operation": "context", "arguments": {"repository": event["cwd"]}},
                timeout=2,
            )
        except Exception:
            context = {
                "active": False,
                "unavailable": True,
                "phase": 0,
                "message": "Coordinator unavailable; do not claim managed protection or issue direct writes.",
            }
        print(json.dumps(decision(event, context)))
        return 0
    except Exception:
        print("Agent Engine hook failed to validate input", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
