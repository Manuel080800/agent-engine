"""Single entry point for source-tree and packaged local coordinator."""

import sys
from pathlib import Path

plugin_root = Path(__file__).resolve().parents[1]
runtime = plugin_root / "runtime"
sys.path.insert(
    0, str(runtime if runtime.is_dir() else plugin_root.parent / "coordinator")
)

from agent_engine_local.transport.cli import main

if __name__ == "__main__":
    raise SystemExit(main())
