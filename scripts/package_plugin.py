#!/usr/bin/env python3
"""Build a self-contained private marketplace; never modifies Codex user settings."""

import argparse
import json
import shutil
from pathlib import Path


def build(destination):
    root = Path(__file__).resolve().parents[1]
    destination = Path(destination).absolute()
    if destination.exists():
        raise SystemExit("Output exists; choose a new output directory")
    plugin = destination / "plugins" / "agent-engine"
    shutil.copytree(
        root / "plugin", plugin, ignore=shutil.ignore_patterns("__pycache__", "*.pyc")
    )
    shutil.copytree(
        root / "coordinator" / "agent_engine_local",
        plugin / "runtime" / "agent_engine_local",
        ignore=shutil.ignore_patterns("__pycache__", "*.pyc"),
    )
    catalog = destination / ".agents" / "plugins"
    catalog.mkdir(parents=True)
    (catalog / "marketplace.json").write_text(
        json.dumps(
            {
                "name": "agent-engine-private",
                "interface": {"displayName": "Agent Engine"},
                "plugins": [
                    {
                        "name": "agent-engine",
                        "source": {"source": "local", "path": "./plugins/agent-engine"},
                        "policy": {
                            "installation": "AVAILABLE",
                            "authentication": "ON_INSTALL",
                        },
                        "category": "Productivity",
                    }
                ],
            },
            indent=2,
        )
        + "\n"
    )
    return destination


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", default="dist/agent-engine")
    print(build(parser.parse_args().out))
