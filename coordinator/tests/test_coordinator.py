import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from agent_engine_local.service.coordinator import Coordinator
from agent_engine_local.common.errors import Conflict, EngineError
from agent_engine_local.handler.filesystem import digest


class CoordinatorTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.base = Path(self.temporary.name)
        self.repo = self.base / "repo"
        self.repo.mkdir()
        (self.repo / "sample.txt").write_text("human work\n")
        (self.repo / "outside.txt").write_text("untouched\n")
        self.home = self.base / "state"
        self.engine = Coordinator(self.home)
        self.addCleanup(lambda: self.engine.close())

    def task(self, scope=None, tests=None, level="Quick"):
        return self.engine.create(
            str(self.repo),
            scope or ["sample.txt"],
            "Update the sample, preserving existing work",
            ["Do not create commits"],
            tests or [[sys.executable, "-c", "raise SystemExit(0)"]],
            level,
        )

    def prepared(self, **kwargs):
        task = self.task(**kwargs)
        self.engine.prepare(task["id"])
        return task["id"]

    def test_full_roundtrip_and_conditional_restore(self):
        # Backup preserves dirty initial contents and original permissions.
        os.chmod(self.repo / "sample.txt", 0o640)
        task = self.prepared()
        self.engine.apply(task, "sample.txt", "managed change\n")
        self.assertEqual(self.engine.verify(task)["state"], "VERIFIED")
        self.assertEqual(self.engine.review(task, [])["state"], "LOCAL_VERIFIED")
        self.engine.restore(task)
        self.assertEqual((self.repo / "sample.txt").read_text(), "human work\n")
        self.assertEqual((self.repo / "sample.txt").stat().st_mode & 0o777, 0o640)
        self.assertEqual(self.engine.release(task)["state"], "CANCELLED")

    def test_remote_completion_cannot_be_forged(self):
        task = self.prepared()
        with self.assertRaises(EngineError):
            self.engine.review(task, [])
        with self.assertRaises(EngineError):
            self.engine.release(task)
        self.engine.verify(task)
        self.engine.review(task, [])
        self.assertEqual(self.engine.release(task)["state"], "LOCAL_DONE_PENDING_SYNC")
        self.assertNotEqual(self.engine.store.get(task)["state"], "COMPLETED")

    def test_standard_and_deep_cannot_use_quick_approval(self):
        for level in ("Standard", "Deep"):
            with self.assertRaises(EngineError):
                self.task(level=level)

    def test_lock_is_shared_across_processes(self):
        # Independent processes cannot acquire a lock already owned by this coordinator.
        self.prepared()
        code = """import sys
from agent_engine_local.service.coordinator import Coordinator
from agent_engine_local.common.errors import Conflict
c = Coordinator(sys.argv[1])
try:
 c.create(sys.argv[2], ['sample.txt'], 'plan', ['rule'], [['true']])
except Conflict:
 sys.exit(23)
finally:
 c.close()
"""
        result = subprocess.run(
            [sys.executable, "-c", code, str(self.home), str(self.repo)],
            capture_output=True,
        )
        self.assertEqual(result.returncode, 23, result.stderr.decode())

    def test_alias_cannot_bypass_lock(self):
        task = self.prepared()
        alias = self.base / "alias"
        alias.symlink_to(self.repo, target_is_directory=True)
        with self.assertRaises(Conflict):
            self.engine.create(str(alias), ["sample.txt"], "plan", ["rule"], [["true"]])
        self.assertEqual(self.engine.context(str(alias))["task_id"], task)

    def test_durable_lock_owner_prevents_new_task_after_restart(self):
        task = self.prepared()
        self.engine.close()
        self.engine = Coordinator(self.home)
        with self.assertRaises(Conflict):
            self.task()
        self.assertEqual(self.engine.resume(task)["state"], "PREPARED")

    def test_checkpoint_survives_restart(self):
        task = self.prepared()
        self.engine.apply(task, "sample.txt", "changed")
        revision = self.engine.store.get(task)["revision"]
        self.engine.close()
        self.engine = Coordinator(self.home)
        resumed = self.engine.resume(task)
        self.assertEqual(resumed["state"], "APPLIED")
        self.assertEqual(resumed["revision"], revision)
        self.engine.verify(task)
        self.engine.review(task, [])
        self.engine.release(task)

    def test_uncertain_write_is_not_replayed(self):
        task = self.prepared()
        with patch(
            "agent_engine_local.handler.filesystem.Repository.replace",
            side_effect=OSError("simulated crash"),
        ):
            with self.assertRaises(OSError):
                self.engine.apply(task, "sample.txt", "changed")
        self.engine.close()
        self.engine = Coordinator(self.home)
        self.assertEqual(self.engine.resume(task)["state"], "UNCERTAIN")
        with self.assertRaises(EngineError):
            self.engine.apply(task, "sample.txt", "changed")
        self.assertEqual((self.repo / "sample.txt").read_text(), "human work\n")

    def test_external_edit_prevents_apply_and_restore(self):
        task = self.prepared()
        self.engine.apply(task, "sample.txt", "managed")
        (self.repo / "sample.txt").write_text("new human work")
        with self.assertRaises(Conflict):
            self.engine.restore(task)
        with self.assertRaises(Conflict):
            self.engine.apply(task, "sample.txt", "overwrite")
        self.assertEqual((self.repo / "sample.txt").read_text(), "new human work")

    def test_out_of_scope_external_edit_blocks_verification(self):
        task = self.prepared()
        (self.repo / "outside.txt").write_text("external")
        with self.assertRaises(Conflict):
            self.engine.verify(task)

    def test_path_traversal_and_git_metadata_are_rejected(self):
        for name in (
            "../escape",
            "/tmp/escape",
            ".git/config",
            "a/../../escape",
            "./sample.txt",
        ):
            with self.assertRaises(EngineError):
                self.task(scope=[name])

    def test_write_outside_scope_is_rejected(self):
        task = self.prepared()
        with self.assertRaises(EngineError):
            self.engine.apply(task, "outside.txt", "bad")
        self.assertEqual((self.repo / "outside.txt").read_text(), "untouched\n")

    def test_symlink_does_not_modify_external_target(self):
        external = self.base / "external"
        external.write_text("external")
        (self.repo / "link").symlink_to(external)
        task = self.task(scope=["link"])
        with self.assertRaises(Conflict):
            self.engine.prepare(task["id"])
        self.assertEqual(external.read_text(), "external")

    def test_symlink_parent_is_rejected(self):
        external = self.base / "external-dir"
        external.mkdir()
        (self.repo / "link").symlink_to(external, target_is_directory=True)
        task = self.task(scope=["link/new.txt"])
        with self.assertRaises(Conflict):
            self.engine.prepare(task["id"])
        self.assertFalse((external / "new.txt").exists())

    def test_hardlink_is_rejected(self):
        os.link(self.repo / "sample.txt", self.repo / "hardlink")
        task = self.task()
        with self.assertRaises(Conflict):
            self.engine.prepare(task["id"])

    def test_create_delete_and_rename_are_reversible(self):
        task = self.prepared(scope=["sample.txt", "new.txt", "renamed.txt"])
        self.engine.apply(task, "new.txt", "created", "create")
        self.engine.apply(
            task, "sample.txt", operation="rename", destination="renamed.txt"
        )
        self.assertFalse((self.repo / "sample.txt").exists())
        self.assertEqual((self.repo / "renamed.txt").read_text(), "human work\n")
        self.engine.apply(task, "new.txt", operation="delete")
        self.engine.restore(task)
        self.assertEqual((self.repo / "sample.txt").read_text(), "human work\n")
        self.assertFalse((self.repo / "new.txt").exists())
        self.assertFalse((self.repo / "renamed.txt").exists())

    def test_create_and_rename_do_not_overwrite(self):
        task = self.prepared(scope=["sample.txt", "outside.txt"])
        with self.assertRaises(Conflict):
            self.engine.apply(task, "sample.txt", "bad", "create")
        with self.assertRaises(Conflict):
            self.engine.apply(
                task, "sample.txt", operation="rename", destination="outside.txt"
            )

    def test_corrupt_backup_cannot_restore_any_file(self):
        task = self.prepared()
        self.engine.apply(task, "sample.txt", "managed")
        saved = self.engine.store.get(task)
        (self.engine.store.home / saved["backups"]["sample.txt"]["blob"]).write_bytes(
            b"corrupt"
        )
        with self.assertRaises(Conflict):
            self.engine.restore(task)
        self.assertEqual((self.repo / "sample.txt").read_text(), "managed")

    def test_quota_failure_occurs_before_edit(self):
        self.engine.backup_quota = 1
        task = self.task()
        with self.assertRaises(EngineError):
            self.engine.prepare(task["id"])
        with self.assertRaises(EngineError):
            self.engine.apply(task["id"], "sample.txt", "bad")
        self.assertEqual((self.repo / "sample.txt").read_text(), "human work\n")

    def test_expired_context_blocks_new_writes(self):
        now = [1000]
        self.engine.clock = lambda: now[0]
        task = self.prepared()
        now[0] += 4 * 3600
        with self.assertRaises(EngineError):
            self.engine.apply(task, "sample.txt", "bad")

    def test_failed_test_blocks_final_review(self):
        task = self.prepared(tests=[[sys.executable, "-c", "raise SystemExit(17)"]])
        checked = self.engine.verify(task)
        self.assertEqual(checked["state"], "VERIFY_FAILED")
        self.assertEqual(checked["evidence"][0]["exit_code"], 17)
        with self.assertRaises(EngineError):
            self.engine.review(task, [])

    def test_stdout_content_is_not_stored_in_events_or_evidence(self):
        secret = "test-secret-that-must-not-be-persisted"
        task = self.prepared(tests=[[sys.executable, "-c", f"print({secret!r})"]])
        checked = self.engine.verify(task)
        evidence = checked["evidence"][0]
        self.assertEqual(evidence["output_hash"], digest((secret + "\n").encode()))
        summaries = self.engine.store._connection.execute(
            "SELECT summary FROM events"
        ).fetchall()
        self.assertNotIn(secret, json.dumps(summaries))
        self.assertNotIn(secret, json.dumps(evidence))

    def test_test_process_is_killed_on_timeout(self):
        task = self.prepared(
            tests=[[sys.executable, "-c", "import time; time.sleep(10)"]]
        )
        checked = self.engine.verify(task, timeout=0.05)
        self.assertEqual(checked["state"], "VERIFY_FAILED")
        self.assertTrue(checked["evidence"][0]["timed_out"])

    def test_tests_cannot_silently_write_repository(self):
        task = self.prepared(
            tests=[
                [
                    sys.executable,
                    "-c",
                    "from pathlib import Path; Path('outside.txt').write_text('side effect')",
                ]
            ]
        )
        with self.assertRaises(Conflict):
            self.engine.verify(task)
        self.assertEqual(self.engine.store.get(task)["state"], "UNCERTAIN")

    def test_correction_budget_survives_restart(self):
        task = self.prepared(tests=[[sys.executable, "-c", "raise SystemExit(1)"]])
        for _ in range(2):
            self.engine.verify(task)
            self.engine.retry(task, "requirement-1")
        self.engine.close()
        self.engine = Coordinator(self.home)
        self.engine.resume(task)
        self.engine.verify(task)
        with self.assertRaises(EngineError):
            self.engine.retry(task, "requirement-1")
        self.assertEqual(self.engine.store.get(task)["state"], "BLOCKED")

    def test_checkpoint_compare_and_swap(self):
        task = self.prepared()
        first = self.engine.store.get(task)
        stale = self.engine.store.get(task)
        self.engine.store.save(first, "TEST_EVENT", "A valid update")
        with self.assertRaises(EngineError):
            self.engine.store.save(stale, "STALE_EVENT", "A stale update")

    def test_completed_write_receipt_is_idempotent_after_restart(self):
        task = self.prepared()
        self.engine.apply(task, "sample.txt", "managed", operation_id="write-1")
        before = (self.repo / "sample.txt").stat()
        revision = self.engine.store.get(task)["revision"]
        self.engine.close()
        self.engine = Coordinator(self.home)
        result = self.engine.apply(
            task, "sample.txt", "managed", operation_id="write-1"
        )
        after = (self.repo / "sample.txt").stat()
        self.assertEqual(
            (before.st_ino, before.st_mtime_ns), (after.st_ino, after.st_mtime_ns)
        )
        self.assertEqual(result["revision"], revision)
        with self.assertRaises(Conflict):
            self.engine.apply(task, "sample.txt", "different", operation_id="write-1")

    def test_backup_directory_inside_repository_is_rejected(self):
        other = Coordinator(self.repo / "local-state")
        try:
            with self.assertRaises(EngineError):
                other.create(
                    str(self.repo), ["sample.txt"], "plan", ["rule"], [["true"]]
                )
        finally:
            other.close()

    def test_state_directory_symlink_is_rejected(self):
        alias = self.base / "state-alias"
        alias.symlink_to(self.home, target_is_directory=True)
        with self.assertRaises(EngineError):
            Coordinator(alias)


if __name__ == "__main__":
    unittest.main()
