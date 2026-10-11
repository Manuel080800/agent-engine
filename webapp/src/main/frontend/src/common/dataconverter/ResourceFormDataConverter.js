import { toLocalDateTime, toInstant } from "./DateDataConverter.js";
export function formDraft(fields, record) {
  return Object.fromEntries(
    fields.map((field) => {
      const value = record?.data[field.name];

      if (field.type === "datetime-local") return [field.name, toLocalDateTime(value)];

      if (Array.isArray(value)) {
        return [field.name, value.join(", ")];
      }

      const fallback = field.type === "checkbox" ? false : (field.options?.[0] ?? "");
      return [field.name, value ?? fallback];
    }),
  );
}

export function formPayload(fields, draft) {
  return Object.fromEntries(
    fields
      .filter((f) => draft[f.name] !== "" && draft[f.name] != null)
      .map((f) => [
        f.name,
        f.type === "list"
          ? String(draft[f.name])
              .split(",")
              .map((v) => v.trim())
              .filter(Boolean)
          : f.type === "number"
            ? Number(draft[f.name])
            : f.type === "datetime-local"
              ? toInstant(draft[f.name])
              : draft[f.name],
      ]),
  );
}
