import hashlib
import json
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path
from agent_engine_local.repository.journal_repository import JournalRepository
from agent_engine_local.service.synchronization import Synchronizer
from agent_engine_local.common.errors import Conflict, EngineError


class FakeClient:
    def __init__(self, clock):
        self.clock = clock
        self.events = {}
        self.offline = False
        self.fail_after_commit = False

    def sign_cache(self, value):
        return hashlib.sha256(("private-test-key" + value).encode()).hexdigest()

    def request(self, path, payload=None):
        if self.offline:
            raise ConnectionError("offline")
        if payload is None:
            return {
                "executionAuthorized": True,
                "expiresAt": datetime.fromtimestamp(
                    self.clock() + 14400, timezone.utc
                ).isoformat(),
                "task": {"id": "task", "version": 1},
            }
        previous = self.events.get(payload["eventId"])
        if previous and previous != payload:
            raise Conflict("identity conflict")
        self.events[payload["eventId"]] = payload
        if self.fail_after_commit:
            self.fail_after_commit = False
            raise ConnectionError("acknowledgment lost")
        return {
            "version": payload["expectedRevision"] + 1,
            "data": {"state": payload["to"]},
        }


class SynchronizationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix="ae")
        self.now = 1000000
        self.store = JournalRepository(Path(self.tmp.name) / "state")
        self.client = FakeClient(lambda: self.now)
        self.sync = Synchronizer(self.store, self.client, lambda: self.now)

    def tearDown(self):
        self.store.close()
        self.tmp.cleanup()

    def event(self, identifier="e", sequence=1):
        return {
            "eventId": identifier,
            "taskId": "task",
            "localSequence": sequence,
            "expectedRevision": sequence,
            "to": "VERIFYING",
        }

    def test_cache_expires_from_last_remote_sync(self):
        self.sync.context("task")
        self.now += 14399
        self.assertTrue(self.sync.context("task", refresh=False)["executionAuthorized"])
        self.now += 1
        with self.assertRaises(EngineError):
            self.sync.context("task", refresh=False)

    def test_modified_cache_or_timestamp_is_rejected(self):
        self.sync.context("task")
        self.store._connection.execute(
            "UPDATE remote_context SET synced_at=synced_at+1"
        )
        self.store._connection.commit()
        with self.assertRaises(Conflict):
            self.sync.context("task", refresh=False)

    def test_lost_acknowledgment_replays_the_identical_event_once(self):
        event = self.event()
        self.sync.enqueue(event)
        self.client.fail_after_commit = True
        with self.assertRaises(ConnectionError):
            self.sync.flush("task")
        self.store.close()
        self.store = JournalRepository(Path(self.tmp.name) / "state")
        self.sync = Synchronizer(self.store, self.client, lambda: self.now)
        self.sync.flush("task")
        self.assertEqual(self.client.events, {"e": event})
        self.assertEqual(
            self.store._connection.execute(
                "SELECT count(*) FROM outbound_events WHERE delivered=0"
            ).fetchone()[0],
            0,
        )

    def test_queue_limit_preserves_existing_events(self):
        with self.store._connection:
            self.store._connection.executemany(
                "INSERT INTO outbound_events VALUES(?,?,?,?,?,0)",
                [
                    (str(i), "task", i, json.dumps(self.event(str(i), i)), self.now)
                    for i in range(10000)
                ],
            )
        with self.assertRaises(EngineError):
            self.sync.enqueue(self.event("last", 10001))
        self.assertEqual(
            self.store._connection.execute(
                "SELECT count(*) FROM outbound_events"
            ).fetchone()[0],
            10000,
        )

    def test_expired_events_require_reconciliation(self):
        self.sync.enqueue(self.event())
        self.now += 30 * 86400 + 1
        with self.assertRaises(Conflict):
            self.sync.flush("task")
        self.assertEqual(self.client.events, {})

    def test_unauthorized_cache_cannot_enable_offline_execution(self):
        document = {
            "executionAuthorized": False,
            "expiresAt": datetime.fromtimestamp(
                self.now + 14400, timezone.utc
            ).isoformat(),
        }
        encoded = json.dumps(document)
        signature = self.client.sign_cache(encoded + str(float(self.now)))
        with self.store._connection:
            self.store._connection.execute(
                "INSERT INTO remote_context VALUES(?,?,?,?)",
                ("task", encoded, self.now, signature),
            )
        with self.assertRaises(EngineError):
            self.sync.context("task", refresh=False)

    def test_capacity_check_and_enqueue_are_atomic_across_connections(self):
        # Dos conexiones no pueden superar la capacidad de la cola entre la consulta y la inserción.
        from concurrent.futures import ThreadPoolExecutor
        from threading import Barrier

        second = JournalRepository(self.store.home)
        barrier = Barrier(2)

        def enqueue(dao, identifier):
            barrier.wait(timeout=5)
            try:
                dao.enqueue(
                    identifier,
                    "task",
                    1 if identifier == "one" else 2,
                    "{}",
                    self.now,
                    1,
                )
                return True
            except EngineError:
                return False

        try:
            with ThreadPoolExecutor(max_workers=2) as executor:
                first = executor.submit(enqueue, self.store.synchronization_dao, "one")
                other = executor.submit(enqueue, second.synchronization_dao, "two")
                self.assertEqual(
                    sorted([first.result(), other.result()]), [False, True]
                )
            self.assertEqual(self.sync.pending_count(), 1)
        finally:
            second.close()
