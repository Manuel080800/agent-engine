"""Sequential native Codex role execution. All proposed edits still use the journal."""

import subprocess
import uuid
from agent_engine_local.common.errors import EngineError
from agent_engine_local.integration.codex_client import CodexClient

SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["summary", "findings", "edits"],
    "properties": {
        "summary": {"type": "string"},
        "findings": {"type": "array", "items": {"type": "string"}},
        "edits": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": ["path", "operation", "content", "destination"],
                "properties": {
                    "path": {"type": "string"},
                    "operation": {
                        "type": "string",
                        "enum": ["create", "modify", "delete", "rename"],
                    },
                    "content": {"type": ["string", "null"]},
                    "destination": {"type": ["string", "null"]},
                },
            },
        },
    },
}


def run(coordinator, task_id, role, consent_id=None, delegation_id=None):
    with coordinator.mutex:
        task, repo = coordinator._active(
            task_id,
            {"PREPARED", "APPLIED", "VERIFIED", "VERIFY_FAILED", "LOCAL_VERIFIED"},
        )
        if "remote" not in task or task.get("active_delegation"):
            raise EngineError("An authenticated idle remote task is required")
        sync = coordinator._sync()
        sync.flush(task["remote"]["taskId"])
        identifier = delegation_id or str(uuid.uuid4())
        permit = sync.client.request(
            "/v1/coordinator/delegations",
            {
                "taskId": task["remote"]["taskId"],
                "blockId": task["remote"]["blockId"],
                "delegationId": identifier,
                "consentId": consent_id,
                "expectedRevision": task["remote"]["revision"],
                "role": role,
            },
        )
        task["remote"]["revision"] = permit["revision"]
        task["active_delegation"] = identifier
        coordinator.store.save(task, "DELEGATION_STARTED", role)
        contract = {
            "taskId": task["remote"]["taskId"],
            "blockId": task["remote"]["blockId"],
            "planId": permit["planId"],
            "planVersion": permit["planVersion"],
            "contextVersion": task["revision"],
            "role": role,
            "goal": task["original_plan"],
            "scope": permit["scope"],
            "requiredRules": task["rules"],
            "references": [],
            "allowedTools": ["read-only code inspection"],
            "acceptanceCriteria": permit["criteria"],
            "modelPolicy": {"modelId": permit["modelId"], "effort": permit["effort"]},
            "approvedScopeVersion": permit["planVersion"],
        }
    try:
        response = CodexClient().execute(
            coordinator.store.home, repo.root, permit, contract, SCHEMA
        )
        with coordinator.mutex:
            task = coordinator.store.get(task_id)
            coordinator._consistent(task, repo)
            result = sync.client.request(
                "/v1/coordinator/delegations/" + identifier + "/finish",
                {
                    "taskId": task["remote"]["taskId"],
                    "revision": task["remote"]["revision"],
                },
            )
            task["remote"]["revision"] = result["version"]
            task.pop("active_delegation", None)
            coordinator.store.save(task, "DELEGATION_FINISHED", role)
        if role not in {"executor", "corrector"} and response["edits"]:
            raise EngineError("Read-only role proposed unauthorized edits")
        for index, edit in enumerate(response["edits"]):
            coordinator.apply(
                task_id, operation_id=identifier + "-" + str(index), **edit
            )
        return {
            "role": role,
            "summary": str(response["summary"])[:2000],
            "findings": response["findings"],
            "proposedEdits": len(response["edits"]),
        }
    except (subprocess.TimeoutExpired, OSError, ValueError) as exc:
        raise EngineError(
            "Delegation interrupted; journal and active delegation retained for reconciliation"
        ) from exc
