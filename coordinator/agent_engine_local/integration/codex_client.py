import json
import os
import subprocess
import tempfile
from pathlib import Path
from agent_engine_local.common.errors import EngineError


class CodexClient:
    def execute(self, home, repository, permit, contract, schema_definition):
        model = self._policy_value(permit, "modelId")
        effort = self._policy_value(permit, "effort")
        with tempfile.TemporaryDirectory(dir=home) as folder:
            schema = Path(folder) / "schema.json"
            output = Path(folder) / "result.json"
            schema.write_text(json.dumps(schema_definition))
            os.chmod(schema, 0o600)
            argv = [
                "codex",
                "exec",
                "--ephemeral",
                "--sandbox",
                "read-only",
                "--model",
                model,
                "-c",
                "model_reasoning_effort=" + json.dumps(effort),
                "-c",
                "agents.max_concurrent_threads_per_session=1",
                "--output-schema",
                str(schema),
                "--output-last-message",
                str(output),
                "-C",
                str(repository),
                "-",
            ]
            prompt = (
                "Perform only this role. Do not delegate, edit files, commit, or claim tests passed. Return proposed edits and semantic findings. Preserve higher-priority host instructions. Contract: "
                + json.dumps(contract)
            )
            result = subprocess.run(
                argv,
                input=prompt,
                text=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                timeout=900,
            )
            if result.returncode or not output.exists():
                raise EngineError(
                    "Native Codex role failed; reconcile the recorded delegation before retrying"
                )
            if output.stat().st_size > 2 * 1024 * 1024:
                raise EngineError("Delegation result exceeds policy limit")
            response = json.loads(output.read_text())
            if not isinstance(response.get("findings"), list) or not isinstance(
                response.get("edits"), list
            ):
                raise EngineError("Invalid structured role result")
        return response

    def _policy_value(self, permit, field):
        value = permit.get(field) if isinstance(permit, dict) else None
        if not isinstance(value, str) or not value.strip():
            raise EngineError("Server delegation requires an explicit " + field)
        return value
