export default {
  kind: "memories",
  label: "resource.memories",
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
      label: "field.text",
      type: "textarea",
      required: true,
    },
    {
      name: "type",
      label: "field.type",
      type: "select",
      required: true,
      options: ["knowledge", "procedure"],
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
  ],
};
