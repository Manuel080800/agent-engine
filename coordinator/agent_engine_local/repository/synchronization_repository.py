class SynchronizationRepository:
    """Simple cache and delivery queries; no workflow or HTTP policy."""

    def __init__(self, connection):
        self._connection = connection

    def save_context(self, task_id, document, synced_at, signature):
        with self._connection:
            self._connection.execute(
                """
                INSERT INTO remote_context VALUES(?,?,?,?)
                ON CONFLICT(task_id) DO UPDATE SET document=excluded.document,
                    synced_at=excluded.synced_at,signature=excluded.signature
                """,
                (task_id, document, synced_at, signature),
            )

    def find_context(self, task_id):
        return self._connection.execute(
            "SELECT document,synced_at,signature FROM remote_context WHERE task_id=?",
            (task_id,),
        ).fetchone()

    def count_pending(self, task_id=None):
        if task_id is None:
            return self._connection.execute(
                "SELECT count(*) FROM outbound_events WHERE delivered=0"
            ).fetchone()[0]
        return self._connection.execute(
            "SELECT count(*) FROM outbound_events WHERE task_id=? AND delivered=0",
            (task_id,),
        ).fetchone()[0]

    def find_pending(self, task_id):
        return self._connection.execute(
            "SELECT id,document,created_at FROM outbound_events WHERE task_id=? AND delivered=0 ORDER BY sequence",
            (task_id,),
        ).fetchall()

    def mark_delivered(self, event_id):
        with self._connection:
            self._connection.execute(
                "UPDATE outbound_events SET delivered=1 WHERE id=?", (event_id,)
            )
