---
name: restore-context
description: Resume an interrupted or compacted Agent Engine task from its durable checkpoint.
---

Read the local context and resume(task_id). Recover exact original plan, active rules/skills versions, block, next action and correction budgets. Never repeat verified blocks or infer the outcome of an UNCERTAIN write. restore(task_id) is conditional on backup integrity and absence of observable external changes. On conflicts preserve both the journal and external edits; require reconciliation. synchronize(task_id) replays stable event identities in sequence with revision CAS. Offline continuation requires a previously authorized context synchronized less than four hours ago; stop when expired or the queue reaches 10000. Local completion remains pending until accepted remotely. New approvals and model starts requiring human verification need connectivity.
