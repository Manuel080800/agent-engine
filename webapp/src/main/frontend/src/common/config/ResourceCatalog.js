import project from "../../project/config/ProjectFormFields.js";
import profile from "../../profile/config/ProfileFormFields.js";
import rule from "../../rule/config/RuleFormFields.js";
import skill from "../../skill/config/SkillFormFields.js";
import model from "../../model/config/ModelFormFields.js";
import policy from "../../model/config/PolicyFormFields.js";
import memory from "../../memory/config/MemoryFormFields.js";
import task from "../../task/config/TaskFormFields.js";
export const catalog = [
  project,
  profile,
  rule,
  skill,
  model,
  policy,
  memory,
  task,
  ...[
    ["plans", "resource.plans"],
    ["blocks", "resource.blocks"],
    ["checkpoints", "resource.checkpoints"],
    ["reviews", "resource.reviews"],
    ["memory-proposals", "resource.memory-proposals"],
    ["local-clients", "resource.local-clients"],
  ].map(([kind, label]) => ({ kind, label, fields: [], editable: false })),
];
