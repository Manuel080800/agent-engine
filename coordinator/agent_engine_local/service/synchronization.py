import hmac
import json
import time
import uuid
from datetime import datetime
from agent_engine_local.common.errors import Conflict, EngineError


class Synchronizer:
    def __init__(self, store, client, clock=time.time):
        self.store = store
        self.client = client
        self.clock = clock
        self.repository = store.synchronization_repository
        self.dao = store.synchronization_dao

    def pending_count(self, task_id=None):
        return self.repository.count_pending(task_id)

    def context(self, task_id, project_id=None, refresh=True):
        if refresh:
            path = (
                "/v1/tasks/"
                + task_id
                + "/context"
                + ("?projectId=" + project_id if project_id else "")
            )
            document = self.client.request(path)
            encoded = json.dumps(document, sort_keys=True, separators=(",", ":"))
            synced = float(self.clock())
            signature = self.client.sign_cache(encoded + str(synced))
            self.repository.save_context(task_id, encoded, synced, signature)
            return document
        row = self.repository.find_context(task_id)
        if not row or self.clock() - row[1] >= 4 * 3600:
            raise EngineError("Remote context expired; reconnect before writing")
        if not hmac.compare_digest(
            row[2], self.client.sign_cache(row[0] + str(row[1]))
        ):
            raise Conflict("Cached context integrity failed")
        document = json.loads(row[0])
        if (
            datetime.fromisoformat(
                document["expiresAt"].replace("Z", "+00:00")
            ).timestamp()
            <= self.clock()
        ):
            raise EngineError("Mandatory context expired")
        if not document.get("executionAuthorized"):
            raise EngineError("Offline execution was not previously authorized")
        return document

    def enqueue(self, event):
        document = json.dumps(event, sort_keys=True, separators=(",", ":"))
        self.dao.enqueue(
            event["eventId"],
            event["taskId"],
            event["localSequence"],
            document,
            self.clock(),
            10000,
        )

    def flush(self, task_id):
        result = None
        for event_id, document, created_at in self.repository.find_pending(task_id):
            if self.clock() - created_at > 30 * 86400:
                raise Conflict(
                    "Queued events exceeded retention; human reconciliation required"
                )
            result = self.client.request("/v1/coordinator/events", json.loads(document))
            self.repository.mark_delivered(event_id)
        return result

    def event(
        self,
        task,
        state,
        evidence=None,
        next_action="Continue the registered plan",
        finding_id=None,
    ):
        remote = task["remote"]
        event = {
            "eventId": str(uuid.uuid4()),
            "taskId": remote["taskId"],
            "blockId": remote["blockId"],
            "expectedRevision": remote["revision"],
            "localSequence": remote["sequence"] + 1,
            "to": state,
            "evidence": evidence,
            "nextAction": next_action,
            "consentId": None,
            "delegationId": None,
            "modelId": None,
            "effort": None,
            "findingId": finding_id,
        }
        self.enqueue(event)
        remote["sequence"] += 1
        remote["revision"] += 1
        remote["state"] = state
        self.store.save(task, "REMOTE_EVENT_QUEUED", state)
        try:
            self.flush(remote["taskId"])
        except ConnectionError:
            pass
        return event
