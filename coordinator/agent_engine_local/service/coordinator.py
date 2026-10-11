import fcntl
import json
import os
import threading
import time
import uuid
from pathlib import Path

from agent_engine_local.common.errors import Conflict, EngineError
from agent_engine_local.handler.filesystem import Repository, digest, relative_path
from agent_engine_local.handler.process_execution import ProcessExecutionHandler
from agent_engine_local.repository.journal_repository import JournalRepository
from agent_engine_local.handler.private_directory import private_directory


class Coordinator:
    """Journaled local execution with independent authenticated remote approval and synchronization.

    Each managed write must go through apply(). Direct shell edits are not managed.
    Tests execute without a shell and their stdout remains in a private temporary file.
    """

    def __init__(self, home, clock=time.time, backup_quota=2 * 1024**3):
        self.store = JournalRepository(home)
        self.clock = clock
        self.process_handler = ProcessExecutionHandler()
        self.backup_quota = backup_quota
        self.mutex = threading.RLock()
        self.locks = {}
        private_directory(self.store.home / "locks")
        private_directory(self.store.home / "backups")

    def run_role(self, task_id, role, consent_id=None, delegation_id=None):
        from agent_engine_local.service.delegation import run

        return run(self, task_id, role, consent_id, delegation_id)

    def _sync(self):
        from agent_engine_local.integration.remote import RemoteClient
        from agent_engine_local.service.synchronization import Synchronizer

        return Synchronizer(self.store, RemoteClient(self.store.home), self.clock)

    def import_remote(self, repository, remote_task_id, tests, project_id=None):
        with self.mutex:
            context = self._sync().context(remote_task_id, project_id)
            if not context.get("executionAuthorized"):
                raise EngineError(
                    "Approve the current plan and rules in the webapp before importing"
                )
            remote = context["task"]
            if remote["data"]["state"] not in {"PLANNED", "WAITING_APPROVAL"}:
                raise Conflict("Remote task is not ready for a new execution")
            blocks = sorted(context["blocks"], key=lambda b: b["data"].get("order", 0))
            if not blocks or any(not b["data"].get("criteria") for b in blocks):
                raise EngineError("Registered blocks and acceptance criteria required")
            plan = context["plan"]["data"]
            rules = [
                str(r["data"]["text"])
                for r in context["rules"]
                if r["data"].get("enabled", True)
            ]
            task = self.create(
                repository,
                plan["scope"],
                plan["original"],
                rules or ["Execute only the approved plan scope"],
                tests,
            )
            task["level"] = remote["data"]["level"]
            task["remote"] = {
                "taskId": remote_task_id,
                "projectId": project_id,
                "blockId": blocks[0]["id"],
                "blockScope": blocks[0]["data"]["scope"],
                "revision": remote["version"],
                "sequence": context.get("localSequence", 0),
                "state": remote["data"]["state"],
                "rulesHash": context["rulesHash"],
            }
            self.store.save(
                task, "REMOTE_TASK_IMPORTED", "Authoritative remote context imported"
            )
            return task

    def synchronize(self, task_id):
        with self.mutex:
            task = self.store.get(task_id)
            if "remote" not in task:
                raise EngineError("Local-only task has no remote identity")
            result = self._sync().flush(task["remote"]["taskId"])
            if result and result["data"]["state"] == "COMPLETED":
                task["completed_at"] = self.clock()
                self._save(task, "COMPLETED", "REMOTE_COMPLETION_CONFIRMED")
                if task["repository_key"] in self.locks:
                    self._unlock(task)
            return task

    def next_block(self, task_id, tests):
        with self.mutex:
            task, repo = self._active(task_id, {"LOCAL_VERIFIED"})
            if "remote" not in task:
                raise EngineError("Multiple blocks require a registered remote plan")
            context = self._sync().context(
                task["remote"]["taskId"], task["remote"]["projectId"]
            )
            blocks = sorted(
                [b for b in context["blocks"] if b["data"].get("state") != "VERIFIED"],
                key=lambda b: b["data"].get("order", 0),
            )
            if not blocks:
                raise EngineError("No remaining blocks; release after final review")
            if (
                not isinstance(tests, list)
                or not tests
                or not all(
                    isinstance(c, list)
                    and c
                    and all(isinstance(v, str) and v for v in c)
                    for c in tests
                )
            ):
                raise EngineError("Bounded verification argv required")
            task["remote"]["blockId"] = blocks[0]["id"]
            task["remote"]["blockScope"] = blocks[0]["data"]["scope"]
            task["tests"] = tests
            task["evidence"] = []
            task["review"] = None
            task["block_corrections"] = 0
            self._consistent(task, repo)
            self._save(task, "APPLIED", "NEXT_BLOCK_STARTED")
            return task

    def collect_backups(self):
        import shutil

        removed = 0
        with self.mutex:
            for task in self.store.list():
                if (
                    task["state"] == "COMPLETED"
                    and self.clock() - task.get("completed_at", self.clock())
                    >= 7 * 86400
                ):
                    folder = self.store.home / "backups" / task["id"]
                    if folder.exists():
                        shutil.rmtree(folder)
                        removed += 1
        return {"removed": removed}

    def _remote_evidence(self, task, kind, passed=False):
        return {
            "kind": kind,
            "snapshotHash": digest(
                json.dumps(task["expected"], sort_keys=True).encode()
            ),
            "backupReady": bool(task["backups"]),
            "tests": [
                {
                    "commandHash": e["command_hash"],
                    "outputHash": e["output_hash"],
                    "exitCode": e["exit_code"],
                    "timedOut": e["timed_out"],
                }
                for e in task["evidence"]
            ],
            "passed": passed,
        }

    def _repo(self, task):
        repo = Repository(task["repository"])
        if repo.key != task["repository_key"]:
            raise Conflict("Repository identity changed")
        return repo

    def _lock(self, task):
        key = task["repository_key"]
        if key in self.locks:
            if self.locks[key][0] != task["id"]:
                raise Conflict("Repository already has an active task")
            return
        path = self.store.home / "locks" / key
        fd = os.open(path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
        try:
            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as exc:
            os.close(fd)
            raise Conflict("Another coordinator holds the repository lock") from exc
        # Prevent another process from adopting a different task after a crash.
        owner = os.read(fd, 1024).decode()
        if owner and owner != task["id"]:
            os.close(fd)
            raise Conflict("Repository has an unresolved durable task owner")
        os.ftruncate(fd, 0)
        os.write(fd, task["id"].encode())
        os.fsync(fd)
        self.locks[key] = (task["id"], fd)

    def _unlock(self, task):
        owner, fd = self.locks.pop(task["repository_key"])
        os.ftruncate(fd, 0)
        os.fsync(fd)
        os.close(fd)

    def _save(self, task, state, kind):
        task["state"] = state
        self.store.save(task, kind, state)

    def _active(self, task_id, states):
        task = self.store.get(task_id)
        if task["state"] not in states:
            raise EngineError("Operation is incompatible with current task state")
        self._lock(task)
        if task.get("active_delegation"):
            raise EngineError("An unresolved delegation owns this task")
        if "remote" in task:
            sync = self._sync()
            try:
                context = sync.context(
                    task["remote"]["taskId"], task["remote"]["projectId"]
                )
            except ConnectionError:
                context = sync.context(task["remote"]["taskId"], refresh=False)
            from datetime import datetime

            task["expires_at"] = datetime.fromisoformat(
                context["expiresAt"].replace("Z", "+00:00")
            ).timestamp()
            if (
                not context.get("executionAuthorized")
                or context["rulesHash"] != task["remote"]["rulesHash"]
                or context["plan"]["data"]["hash"] != task["plan_hash"]
            ):
                raise Conflict(
                    "Remote plan, rules or approval changed; refresh and reconcile"
                )
            if sync.pending_count() >= 10000:
                raise EngineError("Offline queue full; suspend execution")
        if self.clock() >= task["expires_at"]:
            raise EngineError(
                "Local context expired; new writes require a fresh validated context"
            )
        return task, self._repo(task)

    def _consistent(self, task, repo):
        current = repo.snapshot()
        expected = task["expected"]
        if current != expected:
            raise Conflict("Repository changed outside the last managed operation")

    def create(self, repository, scope, plan, rules, tests, level="Quick"):
        with self.mutex:
            if level != "Quick":
                raise EngineError(
                    "Standard/Deep require an authenticated human approval channel; use import_remote after approval in the webapp"
                )
            if not isinstance(plan, str) or not plan.strip() or len(plan) > 10000:
                raise EngineError("A bounded, nonempty plan is required")
            if (
                not isinstance(rules, list)
                or not 0 < len(rules) <= 100
                or not all(isinstance(r, str) and 0 < len(r) <= 2000 for r in rules)
            ):
                raise EngineError("At least one bounded rule is required")
            if not isinstance(scope, list) or not scope or len(scope) > 100:
                raise EngineError("A bounded explicit file scope is required")
            scope = sorted(set(relative_path(name) for name in scope))
            if any(a.startswith(b + "/") for a in scope for b in scope if a != b):
                raise EngineError("Overlapping scope entries are unsupported")
            if not isinstance(tests, list) or not tests or len(tests) > 10:
                raise EngineError("At least one verification command is required")
            for command in tests:
                if (
                    not isinstance(command, list)
                    or not command
                    or not all(
                        isinstance(v, str) and v and len(v) <= 2000 for v in command
                    )
                ):
                    raise EngineError(
                        "Verification commands must be bounded argv arrays"
                    )
            repo = Repository(repository)
            if self.store.home.is_relative_to(repo.root) or repo.root.is_relative_to(
                self.store.home
            ):
                raise EngineError("State and backups must be outside the repository")
            task = {
                "id": str(uuid.uuid4()),
                "repository": str(repo.root),
                "repository_key": repo.key,
                "state": "RECEIVED",
                "scope": scope,
                "original_plan": plan,
                "plan_hash": digest(plan.encode()),
                "rules": rules,
                "tests": tests,
                "created_at": self.clock(),
                "expires_at": self.clock() + 4 * 3600,
                "expected": {},
                "baseline": {},
                "backups": {},
                "evidence": [],
                "review": None,
                "corrections": 0,
                "finding_attempts": {},
                "journal": None,
                "applied_operations": {},
            }
            self._lock(task)
            try:
                self.store.create(task)
            except Exception:
                self._unlock(task)
                raise
            return self.store.get(task["id"])

    def prepare(self, task_id):
        with self.mutex:
            task, repo = self._active(task_id, {"RECEIVED"})
            baseline = repo.snapshot()
            task["baseline"] = baseline
            task["expected"] = baseline
            self._save(task, "BACKUP_PREPARING", "BACKUP_PREPARING")
            folder = private_directory(self.store.home / "backups" / task_id)
            used = sum(
                p.stat().st_size
                for p in (self.store.home / "backups").rglob("*")
                if p.is_file()
            )
            backups = {}
            for index, name in enumerate(task["scope"]):
                before, data = repo.read(name)
                if before != baseline.get(name, {"kind": "missing"}):
                    raise Conflict("File changed while preparing backup")
                record = {"before": before, "blob": None}
                if data is not None:
                    if used + len(data) > self.backup_quota:
                        raise EngineError(
                            "Backup quota exhausted; no edits are permitted"
                        )
                    blob = folder / str(index)
                    with blob.open("xb") as stream:
                        os.chmod(blob, 0o600)
                        stream.write(data)
                        stream.flush()
                        os.fsync(stream.fileno())
                    if digest(blob.read_bytes()) != before["hash"]:
                        raise EngineError("Backup integrity verification failed")
                    record["blob"] = str(blob.relative_to(self.store.home))
                    used += len(data)
                backups[name] = record
            directory_fd = os.open(folder, os.O_RDONLY | os.O_DIRECTORY)
            os.fsync(directory_fd)
            os.close(directory_fd)
            task["backups"] = backups
            self._consistent(task, repo)
            self._save(task, "PREPARED", "BACKUP_READY")
            if "remote" in task:
                self._sync().event(
                    task, "EXECUTING", self._remote_evidence(task, "BACKUP")
                )
            return task

    def apply(
        self,
        task_id,
        path,
        content=None,
        operation="modify",
        destination=None,
        operation_id=None,
    ):
        with self.mutex:
            task = self.store.get(task_id)
            fingerprint = digest(
                json.dumps([path, content, operation, destination]).encode()
            )
            if operation_id is not None:
                if (
                    not isinstance(operation_id, str)
                    or len(operation_id) > 64
                    or not operation_id
                ):
                    raise EngineError("Invalid idempotency identifier")
                previous_operation = task["applied_operations"].get(operation_id)
                if previous_operation:
                    if previous_operation != fingerprint:
                        raise Conflict(
                            "Idempotency identifier reused with different arguments"
                        )
                    # Return a receipt without touching the filesystem or reacquiring a released lock.
                    return task
            task, repo = self._active(task_id, {"PREPARED", "APPLIED"})
            if operation not in {"create", "modify", "delete", "rename"}:
                raise EngineError("Unsupported operation")
            names = [relative_path(path)]
            if operation == "rename":
                names.append(relative_path(destination))
            if any(name not in task["scope"] for name in names) or len(
                set(names)
            ) != len(names):
                raise EngineError("Operation outside approved scope")
            if "remote" in task and any(
                name not in task["remote"]["blockScope"] for name in names
            ):
                raise EngineError(
                    "Write belongs to another block; finish the current block first"
                )
            self._consistent(task, repo)
            previous, data = repo.read(path)
            if operation == "create" and previous["kind"] != "missing":
                raise Conflict("Create would overwrite an existing file")
            if (
                operation in {"modify", "delete", "rename"}
                and previous["kind"] != "file"
            ):
                raise Conflict("Source does not exist as a regular file")
            if operation in {"create", "modify"}:
                if not isinstance(content, str) or len(content.encode()) > 1024 * 1024:
                    raise EngineError(
                        "Managed edits require UTF-8 content of at most one MiB"
                    )
                data = content.encode()
            changes = [
                {
                    "path": path,
                    "before": previous,
                    "after": (
                        None if operation in {"delete", "rename"} else digest(data)
                    ),
                }
            ]
            if operation == "rename":
                target = repo.read(destination)[0]
                if target["kind"] != "missing":
                    raise Conflict("Rename target already exists")
                changes.insert(
                    0, {"path": destination, "before": target, "after": digest(data)}
                )
            task["journal"] = {
                "operation": operation,
                "operation_id": operation_id,
                "changes": changes,
            }
            task["evidence"] = []
            task["review"] = None
            self._save(task, "APPLYING", "APPLYING")
            try:
                for change in changes:
                    desired = data if change["after"] is not None else None
                    result = repo.replace(
                        change["path"],
                        change["before"],
                        desired,
                        previous.get("mode", 0o644),
                        previous.get("attributes", {}),
                    )
                    if result["kind"] == "missing":
                        task["expected"].pop(change["path"], None)
                    else:
                        task["expected"][change["path"]] = result
                self._consistent(task, repo)
                if operation_id:
                    task["applied_operations"][operation_id] = fingerprint
                self._save(task, "APPLIED", "APPLIED")
            except Exception:
                self._save(task, "UNCERTAIN", "WRITE_UNCERTAIN")
                raise
            return task

    def verify(self, task_id, timeout=30):
        with self.mutex:
            if not isinstance(timeout, (int, float)) or not 0 < timeout <= 120:
                raise EngineError("Verification timeout must be within 120 seconds")
            task, repo = self._active(task_id, {"PREPARED", "APPLIED", "VERIFY_FAILED"})
            self._consistent(task, repo)
            self._save(task, "VERIFYING", "VERIFYING")
            evidence = []
            try:
                for argv in task["tests"]:
                    evidence.append(
                        self.process_handler.run(
                            argv, repo.root, self.store.home, timeout
                        )
                    )
                self._consistent(task, repo)
                task["evidence"] = evidence
                task["verified_snapshot_hash"] = digest(
                    json.dumps(task["expected"], sort_keys=True).encode()
                )
                passed = all(
                    e["exit_code"] == 0 and not e["timed_out"] for e in evidence
                )
                self._save(
                    task,
                    "VERIFIED" if passed else "VERIFY_FAILED",
                    "VERIFICATION_RECORDED",
                )
                if "remote" in task:
                    self._sync().event(
                        task,
                        "VERIFYING",
                        self._remote_evidence(task, "PROCESS", passed),
                    )
            except Exception:
                self._save(task, "UNCERTAIN", "VERIFICATION_UNCERTAIN")
                raise
            return task

    def retry(self, task_id, finding):
        with self.mutex:
            task, repo = self._active(task_id, {"VERIFY_FAILED"})
            self._consistent(task, repo)
            if not isinstance(finding, str) or not 0 < len(finding) <= 200:
                raise EngineError("A stable bounded finding identifier is required")
            count = task["finding_attempts"].get(finding, 0)
            if (
                count >= 2
                or task.get("block_corrections", task["corrections"]) >= 3
                or task["corrections"] >= 6
            ):
                self._save(task, "BLOCKED", "CORRECTION_BUDGET_EXHAUSTED")
                if "remote" in task:
                    self._sync().event(
                        task,
                        "BLOCKED",
                        self._remote_evidence(task, "PROCESS"),
                        "Human budget review required",
                    )
                raise EngineError("Correction budget exhausted")
            task["finding_attempts"][finding] = count + 1
            task["corrections"] += 1
            task["block_corrections"] = task.get("block_corrections", 0) + 1
            self._save(task, "APPLIED", "CORRECTION_STARTED")
            if "remote" in task:
                self._sync().event(
                    task,
                    "CORRECTING",
                    self._remote_evidence(task, "BACKUP"),
                    finding_id=str(uuid.UUID(finding)),
                )
            return task

    def review(self, task_id, findings):
        with self.mutex:
            task, repo = self._active(task_id, {"VERIFIED"})
            self._consistent(task, repo)
            if (
                not isinstance(findings, list)
                or len(findings) > 100
                or not all(isinstance(f, str) and len(f) <= 1000 for f in findings)
            ):
                raise EngineError("Review requires bounded findings")
            # Semantic review is an assertion, not a server-certified fact.
            task["review"] = {
                "origin": "MODEL_REPORTED",
                "findings": findings,
                "plan_hash": task["plan_hash"],
                "snapshot_hash": task["verified_snapshot_hash"],
            }
            self._save(
                task,
                "BLOCKED" if findings else "LOCAL_VERIFIED",
                "FINAL_REVIEW_RECORDED",
            )
            if "remote" in task:
                if findings:
                    self._sync().event(
                        task,
                        "BLOCKED",
                        self._remote_evidence(task, "BLOCK_REVIEW"),
                        "Reconcile blocking semantic findings",
                    )
                else:
                    context = self._sync().context(
                        task["remote"]["taskId"], refresh=False
                    )
                    others = [
                        b
                        for b in context["blocks"]
                        if b["id"] != task["remote"]["blockId"]
                        and b["data"].get("state") != "VERIFIED"
                    ]
                    self._sync().event(
                        task,
                        "EXECUTING" if others else "FINAL_REVIEW",
                        self._remote_evidence(task, "BLOCK_REVIEW", True),
                        (
                            "Proceed to the next block"
                            if others
                            else "Perform final plan review"
                        ),
                    )
            return task

    def release(self, task_id):
        with self.mutex:
            task, repo = self._active(task_id, {"LOCAL_VERIFIED", "RESTORED"})
            self._consistent(task, repo)
            if (
                "remote" in task
                and task["state"] == "LOCAL_VERIFIED"
                and task["remote"]["state"] != "FINAL_REVIEW"
            ):
                raise EngineError("Complete every registered block before releasing")
            self._save(
                task,
                (
                    "LOCAL_DONE_PENDING_SYNC"
                    if task["state"] == "LOCAL_VERIFIED"
                    else "CANCELLED"
                ),
                "LOCAL_RELEASED",
            )
            if "remote" in task and task["state"] == "LOCAL_DONE_PENDING_SYNC":
                if task["remote"]["state"] != "FINAL_REVIEW":
                    raise EngineError(
                        "Complete every registered block before releasing"
                    )
                self._sync().event(
                    task,
                    "COMPLETED",
                    self._remote_evidence(task, "FINAL_REVIEW", True),
                    "Await human memory decisions",
                )
                try:
                    result = self._sync().flush(task["remote"]["taskId"])
                except ConnectionError:
                    return task
                pending = self._sync().pending_count(task["remote"]["taskId"])
                if not pending:
                    task["completed_at"] = self.clock()
                    self._save(task, "COMPLETED", "REMOTE_COMPLETION_CONFIRMED")
            self._unlock(task)
            return task

    def restore(self, task_id):
        with self.mutex:
            task, repo = self._active(
                task_id,
                {
                    "PREPARED",
                    "APPLIED",
                    "VERIFIED",
                    "VERIFY_FAILED",
                    "LOCAL_VERIFIED",
                    "BLOCKED",
                },
            )
            self._consistent(task, repo)
            # Validate all backups before restoring any file.
            contents = {}
            for name, backup in task["backups"].items():
                data = None
                if backup["blob"]:
                    blob = self.store.home / backup["blob"]
                    if blob.is_symlink() or not blob.is_relative_to(self.store.home):
                        raise Conflict("Unsafe backup path")
                    data = blob.read_bytes()
                    if digest(data) != backup["before"]["hash"]:
                        raise Conflict("Backup integrity failure")
                contents[name] = data
            self._save(task, "RESTORING", "RESTORE_PREPARING")
            try:
                for name, data in contents.items():
                    expected = task["expected"].get(name, {"kind": "missing"})
                    initial = task["backups"][name]["before"]
                    if expected == initial:
                        continue
                    result = repo.replace(
                        name,
                        expected,
                        data,
                        initial.get("mode", 0o644),
                        initial.get("attributes", {}),
                    )
                    if result["kind"] == "missing":
                        task["expected"].pop(name, None)
                    else:
                        task["expected"][name] = result
                self._consistent(task, repo)
                self._save(task, "RESTORED", "RESTORE_DONE")
            except Exception:
                self._save(task, "UNCERTAIN", "RESTORE_UNCERTAIN")
                raise
            return task

    def resume(self, task_id):
        with self.mutex:
            task = self.store.get(task_id)
            if task["state"] in {"CANCELLED", "LOCAL_DONE_PENDING_SYNC", "COMPLETED"}:
                return task
            self._lock(task)
            repo = self._repo(task)
            if task["state"] in {
                "BACKUP_PREPARING",
                "APPLYING",
                "VERIFYING",
                "RESTORING",
                "UNCERTAIN",
            }:
                task["recovery_snapshot_hash"] = digest(
                    json.dumps(repo.snapshot(), sort_keys=True).encode()
                )
                self._save(task, "UNCERTAIN", "RECOVERY_REQUIRES_RECONCILIATION")
                # Never replay or infer an uncertain operation's success from hash alone.
                return task
            if task["state"] != "RECEIVED":
                self._consistent(task, repo)
            return task

    def context(self, repository):
        with self.mutex:
            key = Repository(repository).key
            tasks = [
                t
                for t in self.store.list()
                if t["repository_key"] == key
                and (
                    t["state"] not in {"CANCELLED", "COMPLETED"}
                    and (t["state"] != "LOCAL_DONE_PENDING_SYNC" or "remote" in t)
                )
            ]
            if not tasks:
                return {"active": False, "phase": 0}
            if len(tasks) != 1:
                raise Conflict("Multiple unresolved tasks require reconciliation")
            task = tasks[0]
            return {
                "active": True,
                "task_id": task["id"],
                "state": task["state"],
                "scope": task["scope"],
                "plan_hash": task["plan_hash"],
                "rules": task["rules"],
                "original_plan": task["original_plan"],
                "revision": task["revision"],
                "expires_at": task["expires_at"],
                "phase": 0,
            }

    def close(self):
        with self.mutex:
            for _, fd in self.locks.values():
                # Preserve durable ownership; restart must explicitly resume this task.
                os.close(fd)
            self.locks.clear()
            self.store.close()
