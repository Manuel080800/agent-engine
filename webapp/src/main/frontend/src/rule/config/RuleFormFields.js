export default {
  kind: "rules",
  label: "resource.rules",
  editable: true,
  fields: [
    {
      name: "name",
      label: "field.name",
      type: "text",
      required: true,
    },
    {
      name: "text",
      label: "field.rule",
      type: "textarea",
      required: true,
    },
    {
      name: "scope",
      label: "field.scope",
      type: "select",
      required: true,
      options: ["user", "personal-project", "global", "repository", "skill", "default"],
    },
    {
      name: "module",
      label: "field.module",
      type: "text",
      required: false,
    },
    {
      name: "required",
      label: "field.required",
      type: "checkbox",
      required: false,
    },
    {
      name: "priority",
      label: "field.priority",
      type: "number",
      required: false,
    },
    {
      name: "source",
      label: "field.source",
      type: "text",
      required: false,
    },
    {
      name: "expiresAt",
      label: "field.expiresAt",
      type: "datetime-local",
      required: false,
    },
    {
      name: "enabled",
      label: "field.enabledRule",
      type: "checkbox",
      required: false,
    },
  ],
};
