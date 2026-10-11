---
name: create-plan
description: Register an immutable plan and ordered execution blocks for an Agent Engine task.
---

Create the original plan using authenticated MCP create_resource kind plans, with taskId, name, original, explicit relative file scope and acceptanceCriteria. Create blocks with taskId, name, order, scope and criteria. Keep blocks sequential and within the plan scope. Never replace the original source; register amendments separately using amendmentOf. Standard and Deep require a human webapp approval bound to the current plan, scope and rules. A model assertion of approval is insufficient. Quick respects native permissions.
