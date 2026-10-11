import hashlib
import json
import sys
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path
from agent_engine_local.service.coordinator import Coordinator
from agent_engine_local.service.synchronization import Synchronizer
from agent_engine_local.handler.filesystem import digest
from agent_engine_local.common.errors import EngineError


class Remote:
    def __init__(self):
        self.offline = False
        self.sequence = 0
        self.revision = 4
        self.state = "PLANNED"
        self.events = {}
        self.blocks = [
            {
                "id": "block-a",
                "data": {
                    "order": 0,
                    "scope": ["a.txt"],
                    "criteria": ["a changed"],
                    "state": "PENDING",
                },
            },
            {
                "id": "block-b",
                "data": {
                    "order": 1,
                    "scope": ["b.txt"],
                    "criteria": ["b changed"],
                    "state": "PENDING",
                },
            },
        ]

    def sign_cache(self, value):
        return hashlib.sha256(("test-key" + value).encode()).hexdigest()

    def request(self, path, payload=None):
        if self.offline:
            raise ConnectionError("offline")
        if payload is None:
            return {
                "executionAuthorized": True,
                "rulesHash": "r" * 64,
                "localSequence": self.sequence,
                "expiresAt": datetime.now(timezone.utc).replace(year=2030).isoformat(),
                "task": {
                    "id": "remote",
                    "version": self.revision,
                    "data": {"level": "Standard", "state": self.state},
                },
                "plan": {
                    "data": {
                        "original": "Original immutable plan",
                        "hash": digest(b"Original immutable plan"),
                        "scope": ["a.txt", "b.txt"],
                    }
                },
                "blocks": json.loads(json.dumps(self.blocks)),
                "rules": [{"data": {"text": "Do not commit", "enabled": True}}],
            }
        if payload["eventId"] in self.events:
            return {"version": self.revision, "data": {"state": self.state}}
        if (
            payload["expectedRevision"] != self.revision
            or payload["localSequence"] != self.sequence + 1
        ):
            raise EngineError("CAS conflict")
        self.events[payload["eventId"]] = payload
        self.revision += 1
        self.sequence += 1
        self.state = payload["to"]
        if (
            payload["evidence"]["kind"] == "BLOCK_REVIEW"
            and payload["evidence"]["passed"]
        ):
            for block in self.blocks:
                if block["id"] == payload["blockId"]:
                    block["data"]["state"] = "VERIFIED"
        if self.state == "COMPLETED" and any(
            b["data"]["state"] != "VERIFIED" for b in self.blocks
        ):
            raise EngineError("Pending block")
        return {"version": self.revision, "data": {"state": self.state}}


class RemoteExecutionTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix="ae")
        self.repository = Path(self.tmp.name) / "repo"
        self.repository.mkdir()
        (self.repository / "a.txt").write_text("a")
        (self.repository / "b.txt").write_text("b")
        self.coordinator = Coordinator(Path(self.tmp.name) / "home")
        self.remote = Remote()
        self.coordinator._sync = lambda: Synchronizer(
            self.coordinator.store, self.remote
        )
        self.tests = [[sys.executable, "-c", "pass"]]

    def tearDown(self):
        self.coordinator.close()
        self.tmp.cleanup()

    def test_approved_standard_task_executes_blocks_sequentially(self):
        task = self.coordinator.import_remote(
            str(self.repository), "remote", self.tests
        )
        identifier = task["id"]
        self.coordinator.prepare(identifier)
        with self.assertRaises(EngineError):
            self.coordinator.apply(identifier, "b.txt", "too early")
        self.coordinator.apply(identifier, "a.txt", "changed a")
        self.coordinator.verify(identifier)
        self.coordinator.review(identifier, [])
        with self.assertRaises(EngineError):
            self.coordinator.release(identifier)
        self.coordinator.next_block(identifier, self.tests)
        self.coordinator.apply(identifier, "b.txt", "changed b")
        self.coordinator.verify(identifier)
        self.coordinator.review(identifier, [])
        result = self.coordinator.release(identifier)
        self.assertEqual(result["state"], "COMPLETED")
        self.assertEqual(self.remote.state, "COMPLETED")
        self.assertEqual(
            [b["data"]["state"] for b in self.remote.blocks], ["VERIFIED", "VERIFIED"]
        )

    def test_offline_completion_waits_for_remote_gate(self):
        self.remote.blocks = self.remote.blocks[:1]
        self.remote.blocks[0]["data"]["scope"] = ["a.txt"]
        task = self.coordinator.import_remote(
            str(self.repository), "remote", self.tests
        )
        identifier = task["id"]
        self.coordinator.prepare(identifier)
        self.remote.offline = True
        self.coordinator.apply(identifier, "a.txt", "changed")
        self.coordinator.verify(identifier)
        self.coordinator.review(identifier, [])
        result = self.coordinator.release(identifier)
        self.assertEqual(result["state"], "LOCAL_DONE_PENDING_SYNC")
        self.assertEqual(self.remote.state, "EXECUTING")
        self.assertTrue(self.coordinator.context(str(self.repository))["active"])
        self.remote.offline = False
        result = self.coordinator.synchronize(identifier)
        self.assertEqual(result["state"], "COMPLETED")
        self.assertFalse(self.coordinator.context(str(self.repository))["active"])
