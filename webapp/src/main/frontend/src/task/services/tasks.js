import { request } from "../../common/services/http.js";
export const tasks = {
  context: (task) =>
    request(
      "/tasks/" + task.id + "/context" + (task.projectId ? "?projectId=" + task.projectId : ""),
    ),
  approve: (task) =>
    request("/tasks/" + task.id + "/approval", {
      method: "POST",
      body: JSON.stringify({ version: task.version }),
    }),
  consent: (task, data) =>
    request("/tasks/" + task.id + "/consent", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  decide: (proposal, decision, editedText) =>
    request("/memory-proposals/" + proposal.id + "/decision", {
      method: "POST",
      body: JSON.stringify({ version: proposal.version, decision, editedText }),
    }),
};
