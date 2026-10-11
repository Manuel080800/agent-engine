import { request, setCsrf } from "../../common/services/http.js";
export async function session() {
  const result = await request("/session");
  setCsrf(result.csrfToken);
  return result;
}
export const register = (mode, data) =>
  request("/auth/" + mode, { method: "POST", body: JSON.stringify(data) });
export const invitation = () => request("/admin/invitations", { method: "POST" });
export async function logout() {
  const result = await session();
  await fetch("/logout", {
    method: "POST",
    credentials: "same-origin",
    headers: { "X-XSRF-TOKEN": result.csrfToken },
  });
  location.reload();
}
