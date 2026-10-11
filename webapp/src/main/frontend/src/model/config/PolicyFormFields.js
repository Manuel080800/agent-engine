export default {
  kind: "model-policies",
  label: "resource.model-policies",
  editable: true,
  fields: [
    { name: "name", label: "field.name", required: true },
    {
      name: "role",
      label: "field.role",
      type: "select",
      options: [
        "analyzer",
        "planner",
        "executor",
        "tester",
        "reviewer",
        "corrector",
        "final-reviewer",
      ],
    },
    {
      name: "modelId",
      label: "field.modelId",
      type: "text",
      required: true,
    },
    {
      name: "effort",
      label: "field.effort",
      type: "text",
      required: true,
    },
  ],
};
