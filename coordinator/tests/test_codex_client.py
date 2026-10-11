import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from agent_engine_local.common.errors import EngineError
from agent_engine_local.integration.codex_client import CodexClient


class CodexClientTests(unittest.TestCase):
    def test_each_execution_uses_the_current_server_model_and_effort(self):
        # Cada ejecución usa la política recibida, incluidos modelos y esfuerzos nuevos.
        with tempfile.TemporaryDirectory() as home:
            observed = []

            def execute(argv, **kwargs):
                observed.append(argv)
                output = Path(argv[argv.index("--output-last-message") + 1])
                output.write_text(json.dumps({"summary": "Reviewed", "findings": [], "edits": []}))
                return SimpleNamespace(returncode=0)

            policies = [
                {"modelId": "custom-engine", "effort": "extended"},
                {"modelId": "another-engine", "effort": "minimal"},
            ]
            with patch("agent_engine_local.integration.codex_client.subprocess.run", side_effect=execute):
                for policy in policies:
                    result = CodexClient().execute(home, home, policy, {}, {})
                    self.assertEqual(result["summary"], "Reviewed")

            for argv, policy in zip(observed, policies):
                self.assertEqual(argv[argv.index("--model") + 1], policy["modelId"])
                self.assertIn("model_reasoning_effort=" + json.dumps(policy["effort"]), argv)

    def test_missing_or_invalid_policy_never_launches_a_default_model(self):
        # Una política incompleta o inválida bloquea la ejecución sin usar un modelo por defecto.
        policies = [
            None,
            {},
            {"modelId": "custom-engine"},
            {"effort": "extended"},
            {"modelId": "", "effort": "extended"},
            {"modelId": "custom-engine", "effort": "  "},
            {"modelId": 1, "effort": "extended"},
            {"modelId": "custom-engine", "effort": ["extended"]},
        ]
        with patch("agent_engine_local.integration.codex_client.subprocess.run") as execute:
            for policy in policies:
                with self.subTest(policy=policy), self.assertRaises(EngineError):
                    CodexClient().execute(None, None, policy, {}, {})
            execute.assert_not_called()

    def test_failed_model_execution_is_not_retried_with_another_model(self):
        # El fallo del modelo configurado no activa una sustitución local silenciosa.
        with tempfile.TemporaryDirectory() as home:
            with patch("agent_engine_local.integration.codex_client.subprocess.run", return_value=SimpleNamespace(returncode=1)) as execute:
                with self.assertRaises(EngineError):
                    CodexClient().execute(home, home, {"modelId": "unavailable-engine", "effort": "custom"}, {}, {})
                execute.assert_called_once()
                argv = execute.call_args.args[0]
                self.assertEqual(argv[argv.index("--model") + 1], "unavailable-engine")
