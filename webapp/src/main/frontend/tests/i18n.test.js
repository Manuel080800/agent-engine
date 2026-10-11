import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";
import { computed } from "vue";
import { parseProperties } from "../src/common/i18n/PropertiesParser.js";
import {
  configureMessages,
  setLocale,
  t,
  message,
  enumLabel,
  formatDate,
  displayValue,
} from "../src/common/i18n/I18n.js";
import { catalog } from "../src/common/config/ResourceCatalog.js";
import { formDraft, formPayload } from "../src/common/dataconverter/ResourceFormDataConverter.js";
import { request } from "../src/common/services/http.js";

const read = (name) =>
  readFileSync(new URL(`../src/common/i18n/messages_${name}.properties`, import.meta.url), "utf8");
const sources = { es: read("es"), en: read("en") };
configureMessages(sources);

function sourceFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = new URL(entry.name + (entry.isDirectory() ? "/" : ""), directory);
    return entry.isDirectory() ? sourceFiles(path) : /\.(js|vue)$/.test(entry.name) ? [path] : [];
  });
}

test("properties support UTF-8, escaped keys, continuations and reject duplicate keys", () => {
  const parsed = parseProperties(
    "# comment\nhello=Español\nlong=first\\\n  second\nescaped\\ key: \\u00e9\\nend\n",
  );
  assert.equal(parsed.hello, "Español");
  assert.equal(parsed.long, "firstsecond");
  assert.equal(parsed["escaped key"], "é\nend");
  assert.throws(() => parseProperties("key=a\nkey=b"), /Duplicate/);
});

test("bundles cover all static messages, resource labels, fields and selectable enums", () => {
  const es = parseProperties(sources.es);
  const en = parseProperties(sources.en);
  assert.deepEqual(Object.keys(es).sort(), Object.keys(en).sort());
  for (const key of Object.keys(es)) {
    assert.ok(es[key] && en[key], key);
    assert.deepEqual(es[key].match(/\{\w+\}/g), en[key].match(/\{\w+\}/g), key);
  }
  for (const file of sourceFiles(new URL("../src/", import.meta.url))) {
    const source = readFileSync(file, "utf8");
    for (const match of source.matchAll(/(?:\bt\(|\bkey:\s*)["']([^"']+)["']/g))
      assert.ok(es[match[1]], `${file}: ${match[1]}`);
  }
  for (const resource of catalog) {
    assert.ok(es[resource.label]);
    for (const field of resource.fields) {
      assert.ok(es[field.label]);
      for (const option of field.options ?? []) assert.ok(es[`enum.${field.name}.${option}`]);
    }
  }
  for (const state of [
    "RECEIVED",
    "CONTEXT_READY",
    "ANALYZING",
    "PLANNED",
    "WAITING_APPROVAL",
    "EXECUTING",
    "VERIFYING",
    "CORRECTING",
    "FINAL_REVIEW",
    "FINAL_CORRECTING",
    "SUSPENDED",
    "BLOCKED",
    "COMPLETED",
    "PENDING",
    "VERIFIED",
  ])
    assert.ok(es[`enum.state.${state}`]);
});

test("locale changes update enums, dates and existing notifications without changing API payloads", () => {
  setLocale("es");
  const label = computed(() => enumLabel("state", "WAITING_APPROVAL"));
  const notification = {
    key: "notice.invitation",
    parameters: { token: "test" },
    dateParameters: { expiresAt: "2026-10-11T18:45:00Z" },
  };
  const before = message(notification);
  assert.equal(label.value, "Esperando aprobación");
  assert.ok(
    displayValue("skillsSnapshot", [{ data: { enabled: false } }]).includes(
      "Datos: Habilitado: No",
    ),
  );
  setLocale("en-US");
  assert.equal(label.value, "Waiting for approval");
  assert.notEqual(message(notification), before);
  assert.ok(!message(notification).includes("2026-10-11T"));
  const task = catalog.find((item) => item.kind === "tasks");
  assert.equal(
    formPayload(task.fields, { name: "Example", objective: "Keep this text", level: "Deep" }).level,
    "Deep",
  );
  assert.equal(enumLabel("effort", "provider-custom"), "provider-custom");
  assert.equal(displayValue("modelId", "custom-provider"), "custom-provider");
  assert.equal(displayValue("text", "2026-10-11T18:45:00Z"), "2026-10-11T18:45:00Z");
  setLocale("unknown");
  assert.equal(t("auth.login"), "Iniciar sesión");
});

test("dates use the selected culture and timezone; missing and invalid values are safe", () => {
  const value = "2026-10-11T18:45:00Z";
  for (const language of ["es-MX", "en-US"]) {
    setLocale(language);
    assert.equal(
      formatDate(value, { timeZone: "America/Merida" }),
      new Intl.DateTimeFormat(language, {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "America/Merida",
      }).format(new Date(value)),
    );
    assert.equal(formatDate(null), t("value.empty"));
    assert.equal(formatDate("bad"), t("date.invalid"));
    assert.equal(displayValue("expiresAt", value), formatDate(value));
  }
});

test("editing a local date preserves the original instant, milliseconds and UTC offset", () => {
  const memory = catalog.find((item) => item.kind === "memories");
  const value = "2026-10-11T12:45:32.123-06:00";
  const draft = formDraft(memory.fields, {
    data: { name: "Memory", text: "Text", type: "knowledge", expiresAt: value },
  });
  assert.ok(!draft.expiresAt.endsWith("Z"));
  assert.equal(formPayload(memory.fields, draft).expiresAt, new Date(value).toISOString());
  draft.expiresAt = "";
  assert.equal(Object.hasOwn(formPayload(memory.fields, draft), "expiresAt"), false);
});

test("backend errors translate specifically and remain reactive after changing language", async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ detail: "Invitation unavailable" }), { status: 403 });
  try {
    setLocale("es");
    let failure;
    try {
      await request("/auth/redeem");
    } catch (error) {
      failure = error;
    }
    assert.equal(message(failure), "La invitación no está disponible o venció.");
    setLocale("en");
    assert.equal(message(failure), "Invitation unavailable");
    globalThis.fetch = async () =>
      new Response(JSON.stringify({ detail: "Unmapped diagnostic" }), { status: 409 });
    await assert.rejects(request("/models"), (error) => message(error) === t("error.409"));
  } finally {
    globalThis.fetch = original;
  }
});
