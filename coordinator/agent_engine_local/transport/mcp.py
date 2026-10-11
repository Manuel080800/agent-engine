"""Read-only local MCP transport for durable coordinator context."""

import json
import os
import sys

from agent_engine_local import __version__
from agent_engine_local.transport.ipc import MAX_MESSAGE, request

PROTOCOLS = {"2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05"}


def respond(message, home, repository):
    method = message.get("method")
    if "id" not in message:
        return None
    result = {"jsonrpc": "2.0", "id": message["id"]}
    if method == "initialize":
        version = message.get("params", {}).get("protocolVersion")
        result["result"] = {
            "protocolVersion": version if version in PROTOCOLS else "2025-11-25",
            "capabilities": {"tools": {}},
            "serverInfo": {"name": "agent-engine-local", "version": __version__},
        }
    elif method == "ping":
        result["result"] = {}
    elif method == "tools/list":
        result["result"] = {
            "tools": [
                {
                    "name": "get_context",
                    "description": "Read the current local checkpoint and rules. Does not approve or certify execution.",
                    "inputSchema": {
                        "type": "object",
                        "properties": {},
                        "additionalProperties": False,
                    },
                    "annotations": {
                        "readOnlyHint": True,
                        "destructiveHint": False,
                        "openWorldHint": False,
                    },
                }
            ]
        }
    elif method == "tools/call":
        parameters = message.get("params", {})
        if (
            parameters.get("name") != "get_context"
            or parameters.get("arguments", {}) != {}
        ):
            result["error"] = {
                "code": -32602,
                "message": "Only get_context with empty arguments is available",
            }
        else:
            try:
                context = request(
                    home,
                    {"operation": "context", "arguments": {"repository": repository}},
                )
                result["result"] = {
                    "content": [{"type": "text", "text": json.dumps(context)}]
                }
            except Exception:
                result["result"] = {
                    "isError": True,
                    "content": [
                        {
                            "type": "text",
                            "text": "Local coordinator unavailable; managed writes must remain blocked",
                        }
                    ],
                }
    else:
        result["error"] = {"code": -32601, "message": "Method not found"}
    return result


def serve(home):
    repository = os.getcwd()
    while line := sys.stdin.buffer.readline(MAX_MESSAGE + 1):
        try:
            if len(line) > MAX_MESSAGE:
                raise ValueError("oversized")
            message = json.loads(line)
            if not isinstance(message, dict) or message.get("jsonrpc") != "2.0":
                raise ValueError("invalid envelope")
            result = respond(message, home, repository)
        except (ValueError, TypeError, AttributeError):
            result = {
                "jsonrpc": "2.0",
                "id": None,
                "error": {"code": -32700, "message": "Invalid JSON-RPC message"},
            }
        if result is not None:
            print(json.dumps(result), flush=True)
