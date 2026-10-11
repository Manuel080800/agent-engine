export default {
  kind: "projects",
  label: "resource.projects",
  editable: true,
  fields: [
    {
      name: "name",
      label: "field.name",
      type: "text",
      required: true,
    },
    {
      name: "description",
      label: "field.description",
      type: "textarea",
      required: false,
    },
    {
      name: "reference",
      label: "field.reference",
      type: "text",
      required: false,
    },
  ],
};
