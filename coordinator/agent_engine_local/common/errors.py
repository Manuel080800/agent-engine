class EngineError(Exception):
    """Expected contract or operational failure; messages must not include source content."""


class Conflict(EngineError):
    pass
