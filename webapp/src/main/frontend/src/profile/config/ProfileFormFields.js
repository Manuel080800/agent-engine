export default {
  kind: "profiles",
  label: "resource.profiles",
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
      name: "active",
      label: "field.active",
      type: "checkbox",
      required: false,
    },
  ],
};
