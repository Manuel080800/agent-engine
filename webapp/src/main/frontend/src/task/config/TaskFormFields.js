export default {
  kind: "tasks",
  label: "resource.tasks",
  editable: false,
  creatable: true,
  fields: [
    { name: "name", label: "field.name", required: true },
    { name: "objective", label: "field.objective", type: "textarea", required: true },
    {
      name: "level",
      label: "field.level",
      type: "select",
      options: ["Quick", "Standard", "Deep"],
      required: true,
    },
  ],
};
