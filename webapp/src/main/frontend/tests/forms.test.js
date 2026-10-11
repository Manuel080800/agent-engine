import { test } from "node:test";
import assert from "node:assert/strict";
import { catalog } from "../src/common/config/ResourceCatalog.js";
import { formDraft, formPayload } from "../src/common/dataconverter/ResourceFormDataConverter.js";
test("model forms preserve booleans, normalize efforts and omit unset optional fields", () => {
  const model = catalog.find((item) => item.kind === "models");
  const payload = formPayload(model.fields, {
    name: "Luna",
    modelId: "gpt-6-luna",
    efforts: "low, high, ",
    priority: "2",
    enabled: false,
    requiresVerification: false,
  });
  assert.deepEqual(payload, {
    name: "Luna",
    modelId: "gpt-6-luna",
    efforts: ["low", "high"],
    priority: 2,
    enabled: false,
    requiresVerification: false,
  });
});

test("editing a model preserves false, zero and configurable effort values", () => {
  const model = catalog.find((item) => item.kind === "models");
  const draft = formDraft(model.fields, {
    data: {
      name: "Custom",
      modelId: "custom-provider",
      efforts: ["extended"],
      priority: 0,
      enabled: false,
      requiresVerification: true,
    },
  });

  assert.equal(draft.enabled, false);
  assert.equal(draft.priority, 0);
  assert.equal(draft.efforts, "extended");
  assert.deepEqual(formPayload(model.fields, draft).efforts, ["extended"]);
});
test("task form cannot send workflow state or verification claims", () => {
  const task = catalog.find((item) => item.kind === "tasks");
  assert.deepEqual(
    formPayload(task.fields, {
      name: "Task",
      objective: "Change",
      level: "Standard",
      state: "COMPLETED",
      evidence: { passed: true },
    }),
    { name: "Task", objective: "Change", level: "Standard" },
  );
});
