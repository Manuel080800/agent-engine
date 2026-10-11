import { test } from "node:test";
import assert from "node:assert/strict";
import { effectScope, nextTick, ref } from "vue";
import { answerConfirmation, confirmation } from "../src/common/composables/useConfirmation.js";
import { useResourceWorkspace } from "../src/common/composables/useResourceWorkspace.js";
import { useTaskDetail } from "../src/task/composables/useTaskDetail.js";

function response(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function settle() {
  return new Promise((resolve) => setImmediate(resolve));
}

test("a late resource response cannot replace the currently selected module", async () => {
  const previousFetch = globalThis.fetch;
  const scope = effectScope();
  let finishRules;
  const delayedRules = new Promise((resolve) => {
    finishRules = resolve;
  });

  globalThis.fetch = async (url) => {
    if (url.endsWith("/session")) {
      return response({ userId: "user", administrator: true, csrfToken: "test-token" });
    }

    if (url.includes("/rules?")) {
      return delayedRules;
    }

    if (url.includes("/models?")) {
      return response([{ id: "model", data: { modelId: "custom-provider" } }]);
    }

    return response([]);
  };

  try {
    const workspace = scope.run(() => useResourceWorkspace());
    await workspace.initialize();
    workspace.kind.value = "rules";
    await nextTick();
    workspace.kind.value = "models";
    await nextTick();
    await settle();

    assert.equal(workspace.records.value[0].id, "model");
    finishRules(response([{ id: "stale-rule", data: {} }]));
    await settle();

    assert.equal(workspace.records.value[0].id, "model");
    assert.equal(workspace.loading.value, false);
  } finally {
    scope.stop();
    globalThis.fetch = previousFetch;
  }
});

test("task approval uses the fetched revision and refreshes the detail afterwards", async () => {
  const previousFetch = globalThis.fetch;
  const scope = effectScope();
  const task = ref({ id: "task", version: 1, projectId: null, data: { state: "PLANNED" } });
  let revision = 3;
  let approvedVersion;
  let changes = 0;

  globalThis.fetch = async (url, options) => {
    if (url.endsWith("/approval")) {
      approvedVersion = JSON.parse(options.body).version;
      revision = 4;
      return response({ id: "approval" });
    }

    return response({
      task: { ...task.value, version: revision },
      blocks: [],
      models: [
        { modelId: "custom-provider", efforts: ["extended"], requiresVerification: true },
        { modelId: "ordinary-provider", efforts: ["fast"], requiresVerification: false },
      ],
    });
  };

  try {
    const detail = scope.run(() =>
      useTaskDetail(
        () => task.value,
        () => {
          changes += 1;
        },
      ),
    );
    await settle();

    assert.equal(detail.currentTask.value.version, 3);
    assert.deepEqual(
      detail.verificationModels.value.map((model) => model.modelId),
      ["custom-provider"],
    );
    const approval = detail.approve();
    assert.equal(confirmation.value.key, "confirm.approvePlan");
    answerConfirmation(true);
    await approval;

    assert.equal(approvedVersion, 3);
    assert.equal(detail.currentTask.value.version, 4);
    assert.equal(changes, 1);
    assert.equal(detail.error.value, "");
  } finally {
    scope.stop();
    globalThis.fetch = previousFetch;
  }
});

test("an anonymous session opens the welcome panel without an error alert", async () => {
  const previousFetch = globalThis.fetch;
  const scope = effectScope();
  globalThis.fetch = async () => response({}, 401);

  try {
    const workspace = scope.run(() => useResourceWorkspace());
    await workspace.initialize();

    assert.equal(workspace.user.value, null);
    assert.equal(workspace.error.value, "");
    assert.equal(workspace.initializing.value, false);
  } finally {
    scope.stop();
    globalThis.fetch = previousFetch;
  }
});

test("canceling a save preserves the editor without sending a mutation", async () => {
  // Cancelar conserva el formulario y no envía cambios al servidor.
  const scope = effectScope();
  const previousFetch = globalThis.fetch;
  let writes = 0;
  globalThis.fetch = async (_url, options) => {
    if (options?.method) writes += 1;
    return response([]);
  };
  try {
    const workspace = scope.run(() => useResourceWorkspace());
    await workspace.edit();
    const save = workspace.save({ name: "Example" });
    assert.equal(confirmation.value.key, "confirm.create");
    answerConfirmation(false);
    await save;
    assert.equal(writes, 0);
    assert.equal(workspace.editing.value, true);
    const acceptedSave = workspace.save({ name: "Example" });
    answerConfirmation(true);
    await acceptedSave;
    assert.equal(writes, 1);
    assert.equal(workspace.editing.value, false);
  } finally {
    answerConfirmation(false);
    scope.stop();
    globalThis.fetch = previousFetch;
  }
});

test("canceling client deletion does not send a request", async () => {
  // La confirmación distingue baja y eliminación definitiva y permite cancelar.
  const scope = effectScope();
  const previousFetch = globalThis.fetch;
  let writes = 0;
  globalThis.fetch = async () => {
    writes += 1;
    return response([]);
  };
  try {
    const workspace = scope.run(() => useResourceWorkspace());
    const row = { id: "client", version: 1, data: { name: "Local" } };
    const deletion = workspace.deleteClient(row);
    assert.equal(confirmation.value.key, "confirm.deleteClient");
    assert.equal(confirmation.value.destructive, true);
    answerConfirmation(false);
    await deletion;
    assert.equal(writes, 0);
    const revoke = workspace.revoke(row);
    assert.equal(confirmation.value.key, "confirm.deactivate");
    answerConfirmation(false);
    await revoke;
    assert.equal(writes, 0);
  } finally {
    answerConfirmation(false);
    scope.stop();
    globalThis.fetch = previousFetch;
  }
});
