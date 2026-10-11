import { ref } from "vue";
import { parseProperties } from "./PropertiesParser.js";

export const locale = ref("es-MX");
const locales = { es: "es-MX", en: "en-US" };
let bundles = {};

export function configureMessages(sources) {
  bundles = Object.fromEntries(
    Object.entries(sources).map(([language, source]) => [language, parseProperties(source)]),
  );
  let preferred;
  try {
    preferred = globalThis.localStorage?.getItem("agentengine.locale");
  } catch {
    /* Storage may be disabled. */
  }
  setLocale(preferred ?? globalThis.navigator?.language ?? "es-MX");
}

export function setLocale(value) {
  locale.value = locales[String(value).split("-")[0]] ?? locales.es;
  if (globalThis.document) document.documentElement.lang = locale.value;
  try {
    globalThis.localStorage?.setItem("agentengine.locale", locale.value);
  } catch {
    /* The selected locale still works for this session. */
  }
}

export function hasTranslation(key) {
  return Object.hasOwn(bundles[locale.value.split("-")[0]] ?? {}, key);
}

export function t(key, parameters = {}) {
  const language = locale.value.split("-")[0];
  const pattern = bundles[language]?.[key] ?? bundles.es?.[key] ?? key;
  return pattern.replace(/\{(\w+)\}/g, (match, name) => String(parameters[name] ?? match));
}

export function message(value) {
  if (!value) return "";
  if (value.key) {
    const parameters = { ...value.parameters };
    if (value.dateParameters) {
      for (const [name, date] of Object.entries(value.dateParameters))
        parameters[name] = formatDate(date);
    }
    return t(value.key, parameters);
  }
  return value instanceof Error ? t("error.network") : String(value);
}

export function enumLabel(field, value) {
  if (value == null || value === "") return t("value.empty");
  const key = `enum.${field}.${value}`;
  // Provider-defined values remain extensible; translation does not constrain the catalog.
  return hasTranslation(key) ? t(key) : String(value);
}

export function formatDate(value, options = {}) {
  if (value == null || value === "") return t("value.empty");
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return t("date.invalid");
  return new Intl.DateTimeFormat(locale.value, {
    dateStyle: "medium",
    timeStyle: "short",
    ...options,
  }).format(date);
}

export function formatNumber(value) {
  return new Intl.NumberFormat(locale.value).format(value);
}

export function fieldLabel(name) {
  const key = `field.${name}`;
  return hasTranslation(key) ? t(key) : name.replace(/([a-z])([A-Z])/g, "$1 $2");
}

export function displayValue(field, value) {
  if (value == null || value === "") return t("value.empty");
  if (typeof value === "boolean") return t(value ? "value.yes" : "value.no");
  if (typeof value === "number") return formatNumber(value);
  if (Array.isArray(value))
    return value
      .map((entry) => displayValue(field === "efforts" ? "effort" : field, entry))
      .join(" · ");
  if (typeof value === "object")
    return Object.entries(value)
      .map(([key, entry]) => `${fieldLabel(key)}: ${displayValue(key, entry)}`)
      .join("\n");
  if (/At$/.test(field)) return formatDate(value);
  return enumLabel(field, value);
}
