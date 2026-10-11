import { request } from "./http.js";
export const resources = {
  list: (kind, filters = {}) =>
    request(
      "/" +
        kind +
        "?" +
        new URLSearchParams(Object.entries(filters).filter(([, v]) => v !== "" && v != null)),
    ),
  save: (kind, record, data, projectId) =>
    request("/" + kind + (record ? "/" + record.id : ""), {
      method: record ? "PUT" : "POST",
      body: JSON.stringify({
        projectId: record ? record.projectId : projectId || null,
        version: record?.version,
        data,
      }),
    }),
  remove: (kind, record) =>
    request("/" + kind + "/" + record.id + "?version=" + record.version, {
      method: "DELETE",
    }),
};
