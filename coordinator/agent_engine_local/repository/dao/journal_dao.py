from agent_engine_local.common.errors import EngineError


class JournalDAO:
    """Atomically persist a checkpoint and its journal event on one connection."""

    def __init__(self, connection):
        self._connection = connection

    def create(self, task_id, document):
        with self._connection:
            self._connection.execute(
                "INSERT INTO tasks VALUES (?, 0, ?)", (task_id, document)
            )
            self._connection.execute(
                "INSERT INTO events(task_id,kind,summary) VALUES (?,?,?)",
                (task_id, "CREATED", "Local task registered"),
            )

    def save(self, task_id, revision, document, kind, summary):
        with self._connection:
            changed = self._connection.execute(
                "UPDATE tasks SET revision=revision+1, document=? WHERE id=? AND revision=?",
                (document, task_id, revision),
            ).rowcount
            if changed != 1:
                raise EngineError("Checkpoint revision conflict")
            self._connection.execute(
                "INSERT INTO events(task_id,kind,summary) VALUES (?,?,?)",
                (task_id, kind, summary),
            )
