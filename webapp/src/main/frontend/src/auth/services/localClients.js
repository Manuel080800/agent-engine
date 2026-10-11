import { t } from "../../common/i18n/I18n.js";
import { request } from "../../common/services/http.js";

export const localClients = {
  link: () =>
    request("/local-clients", {
      method: "POST",
      body: JSON.stringify({
        clientId: "agent-engine-local",
        name: t("client.local"),
      }),
    }),
  remove: (record) =>
    request("/local-clients/" + record.id + "/permanent?version=" + record.version, {
      method: "DELETE",
    }),
  revoke: (record) =>
    request("/local-clients/" + record.id + "?version=" + record.version, {
      method: "DELETE",
    }),
};
