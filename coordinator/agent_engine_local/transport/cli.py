import argparse
import base64
import json
import sys

from agent_engine_local.common.errors import EngineError
from agent_engine_local.transport.ipc import Server, default_home, request


def main():
    parser = argparse.ArgumentParser(description="Agent Engine local coordinator")
    parser.add_argument("--home", default=str(default_home()))
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("serve")
    call = commands.add_parser("request")
    source = call.add_mutually_exclusive_group(required=True)
    source.add_argument("--file", help="JSON envelope path; use - for stdin")
    source.add_argument(
        "--base64", help="Base64 JSON envelope for narrowly authorized hook calls"
    )
    commands.add_parser("mcp")
    config = commands.add_parser("configure")
    config.add_argument("--issuer", required=True)
    config.add_argument("--ca-file")
    commands.add_parser("login")
    args = parser.parse_args()
    try:
        if args.command == "configure":
            from agent_engine_local.integration.remote import configure
            from agent_engine_local.handler.private_directory import private_directory

            configure(private_directory(args.home), args.issuer, args.ca_file)
            print("Remote coordinator configured")
        elif args.command == "login":
            from agent_engine_local.integration.remote import RemoteClient

            print(json.dumps(RemoteClient(args.home).login()))
        elif args.command == "serve":
            server = Server(args.home)
            try:
                server.serve_forever()
            except KeyboardInterrupt:
                pass
            finally:
                server.server_close()
        elif args.command == "mcp":
            from agent_engine_local.transport.mcp import serve

            serve(args.home)
        else:
            if args.base64:
                message = json.loads(base64.b64decode(args.base64, validate=True))
            elif args.file == "-":
                message = json.load(sys.stdin)
            else:
                with open(args.file, encoding="utf-8") as stream:
                    message = json.load(stream)
            print(json.dumps(request(args.home, message)))
    except (EngineError, OSError, ValueError) as exc:
        print(
            str(exc) if isinstance(exc, EngineError) else "Local operation failed",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
