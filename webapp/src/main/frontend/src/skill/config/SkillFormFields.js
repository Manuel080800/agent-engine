export default {
  kind: "skills",
  label: "resource.skills",
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
      label: "field.instructions",
      type: "textarea",
      required: true,
    },
    {
      name: "enabled",
      label: "field.enabledRule",
      type: "checkbox",
      required: false,
    },
  ],
};
