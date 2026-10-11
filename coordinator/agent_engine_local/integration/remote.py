"""Independent PKCE OAuth identity and durable remote event delivery.

Tokens are stored in macOS Keychain. No token is returned through IPC or MCP.
"""

import base64
import hashlib
import hmac
import http.server
import json
import os
import secrets
import shlex
import ssl
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import webbrowser
from pathlib import Path
from agent_engine_local.common.errors import EngineError, Conflict


class Keychain:
    service = "io.agentengine.coordinator.oauth"

    def get(self, account):
        if os.uname().sysname != "Darwin":
            raise EngineError("This credential adapter requires macOS Keychain")
        result = subprocess.run(
            [
                "/usr/bin/security",
                "find-generic-password",
                "-s",
                self.service,
                "-a",
                account,
                "-w",
            ],
            capture_output=True,
            text=True,
        )
        if result.returncode:
            raise EngineError("Coordinator login is required")
        return json.loads(result.stdout)

    def put(self, account, document):
        # The secret travels through stdin, never process argv or application logs.
        command = (
            "add-generic-password -U -s "
            + shlex.quote(self.service)
            + " -a "
            + shlex.quote(account)
            + " -w "
            + shlex.quote(json.dumps(document, separators=(",", ":")))
            + "\n"
        )
        result = subprocess.run(
            ["/usr/bin/security", "-i"], input=command, capture_output=True, text=True
        )
        if result.returncode:
            raise EngineError("Keychain credential storage failed")


class RemoteClient:
    def __init__(self, home, keychain=None):
        self.home = Path(home)
        self.keychain = keychain or Keychain()
        try:
            self.config = json.loads((self.home / "remote.json").read_text())
        except (OSError, ValueError) as exc:
            raise EngineError("Configure the remote coordinator first") from exc
        self.issuer = self.config["issuer"].rstrip("/")
        uri = urllib.parse.urlsplit(self.issuer)
        if (
            uri.scheme != "https"
            or not uri.hostname
            or uri.username
            or uri.password
            or uri.query
            or uri.fragment
        ):
            raise EngineError("A credential-free HTTPS issuer is required")
        self.tls = ssl.create_default_context(cafile=self.config.get("caFile"))

    def _http(self, url, payload=None, form=False, token=None):
        headers = {"Accept": "application/json"}
        data = None
        if payload is not None:
            data = (
                urllib.parse.urlencode(payload) if form else json.dumps(payload)
            ).encode()
            headers["Content-Type"] = (
                "application/x-www-form-urlencoded" if form else "application/json"
            )
        if token:
            headers["Authorization"] = "Bearer " + token
        request = urllib.request.Request(url, data=data, headers=headers)
        try:
            with urllib.request.urlopen(
                request, context=self.tls, timeout=15
            ) as response:
                raw = response.read(2 * 1024 * 1024 + 1)
                if len(raw) > 2 * 1024 * 1024:
                    raise EngineError("Remote response exceeds the context limit")
                return json.loads(raw)
        except urllib.error.HTTPError as exc:
            if exc.code == 409:
                raise Conflict(
                    "Remote revision, scope, approval or event sequence conflict"
                ) from exc
            raise EngineError(
                "Remote request rejected (" + str(exc.code) + ")"
            ) from exc
        except (urllib.error.URLError, TimeoutError) as exc:
            raise ConnectionError("Remote endpoint unavailable") from exc

    def login(self, open_browser=True):
        verifier = secrets.token_urlsafe(48)
        challenge = (
            base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest())
            .decode()
            .rstrip("=")
        )
        state = secrets.token_urlsafe(32)
        callback = {}

        class Handler(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                uri = urllib.parse.urlsplit(self.path)
                params = urllib.parse.parse_qs(uri.query)
                if (
                    uri.path != "/callback"
                    or not hmac.compare_digest(params.get("state", [""])[0], state)
                    or "code" not in params
                ):
                    self.send_error(400)
                    return
                callback["code"] = params["code"][0]
                self.send_response(200)
                self.end_headers()
                self.wfile.write(b"Authorization received. Return to Agent Engine.")

            def log_message(self, *args):
                pass

        url = (
            self.issuer
            + "/oauth2/authorize?"
            + urllib.parse.urlencode(
                {
                    "response_type": "code",
                    "client_id": "agent-engine-local",
                    "redirect_uri": "http://127.0.0.1:8765/callback",
                    "scope": "context:read coordinator:write",
                    "state": state,
                    "code_challenge": challenge,
                    "code_challenge_method": "S256",
                }
            )
        )
        with http.server.HTTPServer(("127.0.0.1", 8765), Handler) as server:
            server.timeout = 2
            if open_browser:
                webbrowser.open(url)
            deadline = time.monotonic() + 180
            while "code" not in callback and time.monotonic() < deadline:
                server.handle_request()
        if "code" not in callback:
            raise EngineError("Loopback authorization expired")
        tokens = self._http(
            self.issuer + "/oauth2/token",
            {
                "grant_type": "authorization_code",
                "client_id": "agent-engine-local",
                "redirect_uri": "http://127.0.0.1:8765/callback",
                "code": callback["code"],
                "code_verifier": verifier,
            },
            form=True,
        )
        self._store(tokens)
        return {"authenticated": True, "issuer": self.issuer}

    def _store(self, tokens):
        if not tokens.get("access_token") or not tokens.get("refresh_token"):
            raise EngineError("Independent rotating coordinator tokens required")
        try:
            previous = self.keychain.get(self.issuer)
        except EngineError:
            previous = {}
        tokens["cache_key"] = previous.get("cache_key") or secrets.token_hex(32)
        tokens["expires_at"] = time.time() + int(tokens.get("expires_in", 900))
        self.keychain.put(self.issuer, tokens)

    def sign_cache(self, value):
        key = bytes.fromhex(self.keychain.get(self.issuer)["cache_key"])
        return hmac.new(key, value.encode(), hashlib.sha256).hexdigest()

    def request(self, path, payload=None):
        if not path.startswith("/v1/") or ".." in path:
            raise EngineError("Restricted coordinator route")
        tokens = self.keychain.get(self.issuer)
        if time.time() >= tokens["expires_at"] - 30:
            tokens = self._http(
                self.issuer + "/oauth2/token",
                {
                    "grant_type": "refresh_token",
                    "client_id": "agent-engine-local",
                    "refresh_token": tokens["refresh_token"],
                },
                form=True,
            )
            self._store(tokens)
        return self._http(self.issuer + path, payload, token=tokens["access_token"])


def configure(home, issuer, ca_file=None):
    document = {"issuer": issuer.rstrip("/")}
    if ca_file:
        document["caFile"] = str(Path(ca_file).resolve(strict=True))
    path = Path(home) / "remote.json"
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC | os.O_NOFOLLOW, 0o600)
    with os.fdopen(fd, "w") as stream:
        json.dump(document, stream)
    return RemoteClient(home)
