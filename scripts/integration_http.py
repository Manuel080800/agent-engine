import base64, hashlib, http.cookiejar, json, re, secrets, ssl, urllib.error, urllib.parse, urllib.request, uuid
from pathlib import Path
import os, time

base = os.environ["AGENT_ENGINE_TEST_URL"].rstrip("/")
bootstrap_token = Path(os.environ["AGENT_ENGINE_TEST_BOOTSTRAP_TOKEN_FILE"]).read_text()
tls = ssl.create_default_context(cafile=os.environ.get("AGENT_ENGINE_TEST_CA_FILE"))
jar = http.cookiejar.CookieJar()
client = urllib.request.build_opener(
    urllib.request.HTTPSHandler(context=tls), urllib.request.HTTPCookieProcessor(jar)
)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


raw = urllib.request.build_opener(
    urllib.request.HTTPSHandler(context=tls),
    urllib.request.HTTPCookieProcessor(jar),
    NoRedirect(),
)


def req(
    path,
    data=None,
    method=None,
    token=None,
    csrf=None,
    form=False,
    expect=200,
    opener=None,
):
    headers = {}
    if csrf:
        headers["X-XSRF-TOKEN"] = csrf
    if token:
        headers["Authorization"] = "Bearer " + token
    if data is not None:
        headers["Content-Type"] = (
            "application/x-www-form-urlencoded" if form else "application/json"
        )
        data = (urllib.parse.urlencode(data) if form else json.dumps(data)).encode()
    try:
        response = (opener or client).open(
            urllib.request.Request(
                base + path, data=data, method=method, headers=headers
            )
        )
        status = response.status
        body = response.read()
        location = response.headers.get("Location")
    except urllib.error.HTTPError as ex:
        status = ex.code
        body = ex.read()
        location = ex.headers.get("Location")
    assert status in (expect if isinstance(expect, tuple) else (expect,)), (
        path,
        status,
        "Unexpected HTTP status",
    )
    if location:
        return location
    try:
        return json.loads(body)
    except ValueError:
        return body.decode()


deadline = time.monotonic() + 60
while True:
    try:
        req("/api/actuator/health")
        break
    except (urllib.error.URLError, AssertionError):
        if time.monotonic() > deadline:
            raise
        time.sleep(1)
email = "test-" + uuid.uuid4().hex + "@example.test"
password = secrets.token_urlsafe(24)
user = req(
    "/api/v1/auth/bootstrap",
    {"email": email, "password": password, "token": bootstrap_token},
)
req(
    "/api/v1/auth/bootstrap",
    {
        "email": "other@example.test",
        "password": password,
        "token": bootstrap_token,
    },
    expect=403,
)
# The localized Vue login keeps Spring form authentication and CSRF protection.
assert 'id="app"' in req("/login")
login_csrf = req("/web/v1/auth/csrf")
assert login_csrf["parameterName"] == "_csrf"
assert req("/login", {"username": email, "password": password}, form=True, expect=302, opener=raw).endswith("/login?expired")
assert req("/login", {"username": email, "password": "incorrect-password", "_csrf": login_csrf["token"]}, form=True, expect=302, opener=raw).endswith("/login?error")
req("/login", {"username": email, "password": password, "_csrf": login_csrf["token"]}, form=True)
index = req("/")
assert "Agent Engine" in index
# The WAR serves the generated Vue JavaScript and stylesheet through its resource handler.
assets = re.findall(r'(?:src|href)="(/assets/[^"]+)"', index)
assert any(asset.endswith(".js") for asset in assets)
assert any(asset.endswith(".css") for asset in assets)
for asset in assets:
    assert req(asset)
assert req("/actuator/health")["status"] == "UP"
session = req("/api/v1/session")
csrf = session["csrfToken"]
assert session["administrator"]
# Webapp and API share the same trusted session through Spring Session JDBC.
assert req("/web/v1/session")["userId"] == user["id"]
# Both transports retain each resource listing; Spring discovers the typed controllers.
for kind in ("projects", "profiles", "rules", "skills", "models", "model-policies", "memories", "tasks", "plans", "blocks", "reviews", "memory-proposals", "checkpoints", "local-clients"):
    assert req("/web/v1/" + kind) == req("/api/v1/" + kind)
req("/api/v1/projects", {"projectId": None, "data": {"name": "No CSRF"}}, expect=403)
project = req(
    "/api/v1/projects",
    {
        "projectId": None,
        "data": {
            "name": "Test project",
            "description": "Disposable integration verification",
        },
    },
    csrf=csrf,
)
# Full-text queries use JPA table mappings, including the irregular policy table name.
search = req("/api/v1/projects?q=integration")
assert any(item["id"] == project["id"] for item in search)
assert req("/api/v1/models?q=luna")
assert req("/api/v1/model-policies?q=unused") == []
req("/api/v1/not-a-resource", expect=404)
req(
    "/api/v1/projects/" + project["id"],
    {"projectId": None, "version": 0, "data": {"name": "Stale update"}},
    method="PUT",
    csrf=csrf,
    expect=409,
)
rule = req(
    "/api/v1/rules",
    {
        "projectId": project["id"],
        "data": {
            "name": "No commits",
            "text": "Never create automatic commits",
            "scope": "personal-project",
            "required": True,
            "enabled": True,
        },
    },
    csrf=csrf,
)
task = req(
    "/api/v1/tasks",
    {
        "projectId": project["id"],
        "data": {
            "name": "Verified task",
            "objective": "Modify a fixture",
            "level": "Standard",
        },
    },
    csrf=csrf,
)
plan = req(
    "/api/v1/plans",
    {
        "projectId": project["id"],
        "data": {
            "name": "Original",
            "taskId": task["id"],
            "original": "Modify fixture and test it",
            "scope": ["fixture.txt"],
            "acceptanceCriteria": ["Fixture tested"],
        },
    },
    csrf=csrf,
)
block = req(
    "/api/v1/blocks",
    {
        "projectId": project["id"],
        "data": {
            "name": "Only block",
            "taskId": task["id"],
            "order": 0,
            "scope": ["fixture.txt"],
            "criteria": ["Fixture tested"],
        },
    },
    csrf=csrf,
)
context = req("/api/v1/tasks/" + task["id"] + "/context?projectId=" + project["id"])
assert not context["executionAuthorized"]
req(
    "/api/v1/tasks/" + task["id"] + "/approval",
    {"version": context["task"]["version"]},
    csrf=csrf,
)
assert req("/api/v1/tasks/" + task["id"] + "/context?projectId=" + project["id"])[
    "executionAuthorized"
]
req(
    "/api/v1/local-clients",
    {"clientId": "agent-engine-local", "name": "Test coordinator"},
    csrf=csrf,
)
audit = req("/api/v1/audit")
assert any(row["resourceId"] == project["id"] for row in audit)
assert all(set(row) == {"operation", "resourceId", "createdAt"} for row in audit)
# Authorization code flow with PKCE and explicit scope consent.
verifier = secrets.token_urlsafe(48)
challenge = (
    base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest())
    .decode()
    .rstrip("=")
)
state = secrets.token_urlsafe(24)
params = {
    "client_id": "agent-engine-local",
    "response_type": "code",
    "redirect_uri": "http://127.0.0.1:8765/callback",
    "scope": "context:read coordinator:write",
    "code_challenge": challenge,
    "code_challenge_method": "S256",
    "state": state,
}
consent = req("/api/oauth2/authorize?" + urllib.parse.urlencode(params))
# The authorization consent form uses parameter-based CSRF protection.
csrf_field = re.search(r'name="_csrf"[^>]*value="([^"]+)"', consent)
consent_state = re.search(r'name="state"[^>]*value="([^"]+)"', consent).group(1)
params = {
    "client_id": "agent-engine-local",
    "state": consent_state,
    "scope": "coordinator:write",
}
if csrf_field:
    params["_csrf"] = csrf_field.group(1)
# Multiple granted scopes require repeated form values.
encoded = urllib.parse.urlencode(params) + "&scope=context%3Aread"
try:
    response = raw.open(
        urllib.request.Request(
            base + "/api/oauth2/authorize",
            data=encoded.encode(),
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
    )
    raise AssertionError("Expected redirect")
except urllib.error.HTTPError as ex:
    assert ex.code == 302, (ex.code, ex.read()[:200])
    callback = ex.headers["Location"]
query = urllib.parse.parse_qs(urllib.parse.urlsplit(callback).query)
assert query["state"][0] == state
code = query["code"][0]
req(
    "/api/oauth2/token",
    {
        "grant_type": "authorization_code",
        "client_id": "agent-engine-local",
        "redirect_uri": "http://127.0.0.1:8765/callback",
        "code": code,
        "code_verifier": "wrong",
    },
    form=True,
    expect=(400, 401),
)
# Request another code because a failed exchange may invalidate the first one.
callback = req(
    "/api/oauth2/authorize?"
    + urllib.parse.urlencode(
        {
            **params,
            "response_type": "code",
            "redirect_uri": "http://127.0.0.1:8765/callback",
            "scope": "context:read coordinator:write",
            "code_challenge": challenge,
            "code_challenge_method": "S256",
        }
    ),
    opener=raw,
    expect=302,
)
code = urllib.parse.parse_qs(urllib.parse.urlsplit(callback).query)["code"][0]
tokens = req(
    "/api/oauth2/token",
    {
        "grant_type": "authorization_code",
        "client_id": "agent-engine-local",
        "redirect_uri": "http://127.0.0.1:8765/callback",
        "code": code,
        "code_verifier": verifier,
    },
    form=True,
)
assert "refresh_token" in tokens
# Two simultaneous refreshes must never both consume the same token.
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier

refresh_barrier = Barrier(2)


def refresh_once():
    opener = urllib.request.build_opener(urllib.request.HTTPSHandler(context=tls))
    refresh_barrier.wait(timeout=10)
    return req(
        "/api/oauth2/token",
        {
            "grant_type": "refresh_token",
            "client_id": "agent-engine-local",
            "refresh_token": tokens["refresh_token"],
        },
        form=True,
        expect=(200, 400),
        opener=opener,
    )


with ThreadPoolExecutor(max_workers=2) as executor:
    outcomes = list(executor.map(lambda _: refresh_once(), range(2)))
successes = [outcome for outcome in outcomes if "access_token" in outcome]
rejections = [
    outcome for outcome in outcomes if outcome.get("error") == "invalid_grant"
]
assert (
    len(successes) == 1 and len(rejections) == 1
), "Concurrent refresh results: " + str(
    [
        "success" if "access_token" in item else item.get("error", "unexpected")
        for item in outcomes
    ]
)
rotated = successes[0]
assert rotated["refresh_token"] != tokens["refresh_token"]
req(
    "/api/oauth2/token",
    {
        "grant_type": "refresh_token",
        "client_id": "agent-engine-local",
        "refresh_token": tokens["refresh_token"],
    },
    form=True,
    expect=400,
)
token = rotated["access_token"]
# A separate client proves webapp JWT validation without relying on the shared session.
bearer_client = urllib.request.build_opener(urllib.request.HTTPSHandler(context=tls))
assert req("/web/v1/models", token=token, opener=bearer_client) == req("/api/v1/models", token=token, opener=bearer_client)
# OAuth issuance belongs exclusively to API; webapp has no token endpoint.
req("/oauth2/token", {"grant_type": "refresh_token"}, token=token, form=True, opener=bearer_client, expect=404)
# MCP protocol and argument conversion are exercised through the transport handler.
listed_tools = req(
    "/api/mcp", {"jsonrpc": "2.0", "id": 1, "method": "tools/list"}, token=token
)
assert listed_tools["result"]["tools"]
invalid_arguments = req(
    "/api/mcp",
    {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/call",
        "params": {
            "name": "list_resources",
            "arguments": {"kind": "projects", "offset": "bad"},
        },
    },
    token=token,
)
assert invalid_arguments["result"]["isError"] is True
invalid_shape = req(
    "/api/mcp",
    {
        "jsonrpc": "2.0",
        "id": 3,
        "method": "tools/call",
        "params": {"name": "list_resources", "arguments": "bad"},
    },
    token=token,
)
assert invalid_shape["result"]["isError"] is True
# New provider names and efforts are accepted through stored configuration.
custom_model = req("/api/v1/models", {
    "projectId": project["id"],
    "data": {"name": "Custom model", "modelId": "custom-engine", "efforts": ["extended"],
             "priority": 1, "enabled": True, "requiresVerification": True},
}, csrf=csrf)
req("/api/v1/model-policies", {
    "projectId": project["id"],
    "data": {"name": "Executor", "role": "executor", "modelId": "custom-engine", "effort": "extended"},
}, csrf=csrf)
req("/api/v1/model-policies", {
    "projectId": project["id"],
    "data": {"name": "Invalid", "role": "reviewer", "modelId": "custom-engine", "effort": "unsupported"},
}, csrf=csrf, expect=400)
snapshot = "a" * 64
process = {
    "commandHash": "b" * 64,
    "outputHash": "c" * 64,
    "exitCode": 0,
    "timedOut": False,
}
sequence = 0
context = req(
    "/api/v1/tasks/" + task["id"] + "/context?projectId=" + project["id"], token=token
)
revision = context["task"]["version"]


def event(to, kind, passed=False):
    global revision, sequence
    sequence += 1
    envelope = {
        "eventId": str(uuid.uuid4()),
        "taskId": task["id"],
        "blockId": block["id"],
        "expectedRevision": revision,
        "localSequence": sequence,
        "to": to,
        "nextAction": "Continue",
        "evidence": {
            "kind": kind,
            "snapshotHash": snapshot,
            "backupReady": True,
            "tests": [process] if passed else [],
            "passed": passed,
        },
        "consentId": None,
        "delegationId": None,
        "modelId": None,
        "effort": None,
    }
    result = req("/api/v1/coordinator/events", envelope, token=token)
    revision = result["version"]
    return envelope, result


# Session-based callers and ordinary MCP clients cannot publish operational events.
req("/api/v1/coordinator/events", {}, csrf=csrf, expect=403)
envelope, result = event("EXECUTING", "BACKUP")
assert req("/api/v1/coordinator/events", envelope, token=token)["version"] == revision
req(
    "/api/v1/coordinator/events",
    {**envelope, "nextAction": "Changed"},
    token=token,
    expect=409,
)
# The configured verification flag applies to custom models and consumes approval once.
delegation_id = str(uuid.uuid4())
delegation = {"taskId": task["id"], "blockId": block["id"], "delegationId": delegation_id,
              "expectedRevision": revision, "role": "executor", "consentId": None}
req("/api/v1/coordinator/delegations", delegation, token=token, expect=403)
consent_result = req("/api/v1/tasks/" + task["id"] + "/consent", {
    "blockId": block["id"], "delegationId": delegation_id, "modelId": "custom-engine", "effort": "extended",
}, csrf=csrf)
delegation["consentId"] = consent_result["id"]
started = req("/api/v1/coordinator/delegations", delegation, token=token)
assert started["modelId"] == "custom-engine" and started["effort"] == "extended"
finished = req("/api/v1/coordinator/delegations/" + delegation_id + "/finish",
               {"taskId": task["id"], "revision": started["revision"]}, token=token)
revision = finished["version"]
delegation["expectedRevision"] = revision
req("/api/v1/coordinator/delegations", delegation, token=token, expect=403)
# Disabling verification in the catalog applies immediately, without recompiling.
custom_model = req("/api/v1/models/" + custom_model["id"], {
    "projectId": project["id"], "version": custom_model["version"],
    "data": {**custom_model["data"], "requiresVerification": False},
}, method="PUT", csrf=csrf)
delegation["delegationId"] = str(uuid.uuid4())
delegation["consentId"] = None
started = req("/api/v1/coordinator/delegations", delegation, token=token)
finished = req("/api/v1/coordinator/delegations/" + delegation["delegationId"] + "/finish",
               {"taskId": task["id"], "revision": started["revision"]}, token=token)
revision = finished["version"]
# A disabled model remains unavailable even when a policy references it.
req("/api/v1/models/" + custom_model["id"], {
    "projectId": project["id"], "version": custom_model["version"],
    "data": {**custom_model["data"], "enabled": False},
}, method="PUT", csrf=csrf)
delegation["expectedRevision"] = revision
delegation["delegationId"] = str(uuid.uuid4())
req("/api/v1/coordinator/delegations", delegation, token=token, expect=400)
event("VERIFYING", "PROCESS", True)
event("FINAL_REVIEW", "BLOCK_REVIEW", True)
event("COMPLETED", "FINAL_REVIEW", True)
assert req("/api/v1/tasks/" + task["id"], token=token)["data"]["state"] == "COMPLETED"
# Source-scope mutation invalidates a previously issued approval.
req(
    "/api/v1/rules/" + rule["id"],
    {
        "projectId": project["id"],
        "version": rule["version"],
        "data": {
            "name": "No commits",
            "text": "Never commit or publish",
            "scope": "personal-project",
            "required": True,
            "enabled": True,
        },
    },
    method="PUT",
    csrf=csrf,
)
assert not req("/api/v1/tasks/" + task["id"] + "/context?projectId=" + project["id"])[
    "executionAuthorized"
]
# JPA ownership and tombstone behavior are verified against the disposable database.
scratch = req(
    "/api/v1/projects",
    {"projectId": None, "data": {"name": "Disposable scratch"}},
    csrf=csrf,
)
req(
    "/api/v1/projects/" + scratch["id"] + "?version=" + str(scratch["version"]),
    method="DELETE",
    csrf=csrf,
    expect=200,
)
req("/api/v1/projects/" + scratch["id"], expect=404)
invitation = req("/api/v1/admin/invitations", {}, csrf=csrf)
second_email = "isolated-" + uuid.uuid4().hex + "@example.test"
second_password = secrets.token_urlsafe(24)
second = req(
    "/api/v1/auth/redeem",
    {"email": second_email, "password": second_password, "token": invitation["token"]},
)
jar = http.cookiejar.CookieJar()
client = urllib.request.build_opener(
    urllib.request.HTTPSHandler(context=tls), urllib.request.HTTPCookieProcessor(jar)
)
login = req("/api/login")
login_csrf = re.search(r'name="_csrf"[^>]*value="([^"]+)"', login).group(1)
req(
    "/api/login",
    {"username": second_email, "password": second_password, "_csrf": login_csrf},
    form=True,
)
assert req("/api/v1/session")["userId"] == second["id"]
req("/api/v1/tasks/" + task["id"], expect=404)
req("/api/v1/projects/" + project["id"], expect=404)
assert req("/api/v1/audit") == []
print(
    "PASS: dynamic model policy/verification and single-use consent, JPA resources and full-text table mappings, typed audit, owner isolation, tombstones, HTTPS, bootstrap one-use, session sharing, CSRF, CAS, approval, PKCE rejection, concurrent single-use refresh/replay, MCP typed arguments, instrumented final gate and approval invalidation"
)
