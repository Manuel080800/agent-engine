#!/usr/bin/env python3
"""Run local safety tests, disposable recovery demo and self-contained package smoke test."""

import json
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "coordinator"))
from package_plugin import build
from verify_layers import verify


def validate():
    verify()
    import os

    environment = dict(
        os.environ, PYTHONPATH=str(ROOT / "coordinator"), PYTHONDONTWRITEBYTECODE="1"
    )
    subprocess.run(
        [
            sys.executable,
            "-m",
            "unittest",
            "discover",
            "-s",
            str(ROOT / "coordinator/tests"),
            "-v",
        ],
        cwd=ROOT,
        env=environment,
        check=True,
    )
    subprocess.run([sys.executable, str(ROOT / "scripts/local_demo.py")], check=True)
    with tempfile.TemporaryDirectory(prefix="ae-pack-") as directory:
        output = build(Path(directory) / "marketplace")
        bridge = output / "plugins/agent-engine/scripts/bridge.py"
        message = {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {"protocolVersion": "2025-11-25"},
        }
        clean_environment = {
            k: os.environ[k]
            for k in ("PATH", "HOME", "LANG", "TMPDIR")
            if k in os.environ
        }
        result = subprocess.run(
            [sys.executable, str(bridge), "mcp"],
            input=json.dumps(message) + "\n",
            cwd=directory,
            env=clean_environment,
            text=True,
            capture_output=True,
            check=True,
        )
        assert (
            json.loads(result.stdout)["result"]["serverInfo"]["name"]
            == "agent-engine-local"
        )
        assert not result.stderr
        assert (
            bridge.parent.parent / "runtime/agent_engine_local/service/coordinator.py"
        ).is_file()
    # Include untracked source files: git diff --check alone skips them.
    for base in (
        ROOT / "coordinator",
        ROOT / "plugin",
        ROOT / "scripts",
        ROOT / "docs",
    ):
        for path in base.rglob("*"):
            if path.is_file() and path.suffix in {".py", ".md", ".json", ".toml"}:
                for index, line in enumerate(path.read_text().splitlines(), 1):
                    if line.rstrip() != line:
                        raise AssertionError(
                            f"Trailing whitespace: {path.relative_to(ROOT)}:{index}"
                        )
    print(
        "PASS: local tests, restart recovery, packaged MCP handshake and whitespace checks"
    )


if __name__ == "__main__":
    validate()
