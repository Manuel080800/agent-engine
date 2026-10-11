import { hasTranslation } from "../i18n/I18n.js";

let csrf = "";
export function setCsrf(value) {
  csrf = value;
}

class RequestError extends Error {
  constructor(key, status, parameters = {}) {
    super(key);
    this.key = key;
    this.status = status;
    this.parameters = parameters;
  }
}

export async function request(path, options = {}) {
  const response = await fetch("/web/v1" + path, {
    credentials: "same-origin",
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(csrf ? { "X-XSRF-TOKEN": csrf } : {}),
      ...options.headers,
    },
  });

  if (response.redirected || response.status === 401) {
    throw new RequestError("error.session", 401);
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const detailKey = `error.backend.${body.detail}`;
    const statusKey = `error.${response.status}`;
    const key = hasTranslation(detailKey)
      ? detailKey
      : hasTranslation(statusKey)
        ? statusKey
        : "error.request";
    throw new RequestError(key, response.status, { status: response.status });
  }

  return response.status === 204 || response.headers.get("content-length") === "0"
    ? null
    : response.json().catch(() => null);
}
