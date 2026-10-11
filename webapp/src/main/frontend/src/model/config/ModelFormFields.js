export default {
  kind: "models",
  label: "resource.models",
  editable: true,
  fields: [
    {
      name: "name",
      label: "field.name",
      type: "text",
      required: true,
    },
    {
      name: "modelId",
      label: "field.modelId",
      type: "text",
      required: true,
    },
    {
      name: "efforts",
      label: "field.efforts",
      type: "list",
      required: true,
    },
    {
      name: "priority",
      label: "field.priority",
      type: "number",
      required: false,
    },
    {
      name: "requiresVerification",
      label: "field.requiresVerification",
      type: "checkbox",
      required: false,
    },
    {
      name: "enabled",
      label: "field.enabled",
      type: "checkbox",
      required: false,
    },
  ],
};
