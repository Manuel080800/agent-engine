import json
import os
import sqlite3
import stat
from pathlib import Path

from agent_engine_local.common.errors import EngineError
from agent_engine_local.handler.private_directory import private_directory
from agent_engine_local.repository.dao.journal_dao import JournalDAO
from agent_engine_local.repository.dao.synchronization_dao import SynchronizationDAO
from agent_engine_local.repository.synchronization_repository import (
    SynchronizationRepository,
)


class JournalRepository:
    def __init__(self, home):
        self.home = private_directory(home)
        path = self.home / "journal.sqlite3"
        fd = os.open(path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
        info = os.fstat(fd)
        os.close(fd)
        if info.st_uid != os.getuid() or info.st_nlink != 1:
            raise EngineError("Unsafe journal file")
        os.chmod(path, 0o600)
        self._connection = sqlite3.connect(path, check_same_thread=False)
        self._connection.execute("PRAGMA journal_mode=WAL")
        self._connection.execute("PRAGMA synchronous=FULL")
        self._connection.executescript("""
            CREATE TABLE IF NOT EXISTS tasks (id TEXT PRIMARY KEY, revision INTEGER NOT NULL, document TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS events (
                sequence INTEGER PRIMARY KEY AUTOINCREMENT, task_id TEXT NOT NULL,
                kind TEXT NOT NULL, summary TEXT NOT NULL);
        """)
        self._connection.commit()
        self.journal_dao = JournalDAO(self._connection)
        self.synchronization_dao = SynchronizationDAO(self._connection)
        self.synchronization_repository = SynchronizationRepository(self._connection)

    def create(self, task):
        self.journal_dao.create(task["id"], json.dumps(task))

    def get(self, task_id):
        row = self._connection.execute(
            "SELECT revision, document FROM tasks WHERE id=?", (task_id,)
        ).fetchone()
        if row is None:
            raise EngineError("Unknown task")
        task = json.loads(row[1])
        task["revision"] = row[0]
        return task

    def save(self, task, kind, summary):
        self.journal_dao.save(
            task["id"], task["revision"], json.dumps(task), kind, summary
        )
        task["revision"] += 1

    def list(self):
        return [
            self.get(row[0])
            for row in self._connection.execute("SELECT id FROM tasks ORDER BY rowid")
        ]

    def close(self):
        self._connection.close()
