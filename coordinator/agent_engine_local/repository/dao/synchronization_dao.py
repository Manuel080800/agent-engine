from agent_engine_local.common.errors import EngineError


class SynchronizationDAO:
    """Schema upgrade and bounded queue insertion are persistence operations."""

    def __init__(self, connection):
        self._connection = connection
        self._connection.executescript("""
            CREATE TABLE IF NOT EXISTS remote_context(
                task_id TEXT PRIMARY KEY, document TEXT NOT NULL, synced_at REAL NOT NULL, signature TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS outbound_events(
                id TEXT PRIMARY KEY, task_id TEXT NOT NULL, sequence INTEGER NOT NULL,
                document TEXT NOT NULL, created_at REAL NOT NULL, delivered INTEGER NOT NULL DEFAULT 0,
                UNIQUE(task_id,sequence));
        """)
        columns = [
            row[1]
            for row in self._connection.execute("PRAGMA table_info(remote_context)")
        ]
        if "signature" not in columns:
            self._connection.execute(
                "ALTER TABLE remote_context ADD COLUMN signature TEXT NOT NULL DEFAULT ''"
            )
        self._connection.commit()

    def enqueue(self, event_id, task_id, sequence, document, created_at, capacity):
        # An IMMEDIATE transaction serializes the capacity check and insertion across connections.
        self._connection.execute("BEGIN IMMEDIATE")
        try:
            pending = self._connection.execute(
                "SELECT count(*) FROM outbound_events WHERE delivered=0"
            ).fetchone()[0]
            if pending >= capacity:
                raise EngineError(
                    "Offline event queue is full; suspend before further writes"
                )
            self._connection.execute(
                "INSERT INTO outbound_events VALUES(?,?,?,?,?,0)",
                (event_id, task_id, sequence, document, created_at),
            )
            self._connection.commit()
        except Exception:
            self._connection.rollback()
            raise
