import { shallowRef } from "vue";

export const confirmation = shallowRef(null);
let resolveConfirmation;

export function confirmAction(key, parameters = {}, destructive = false) {
  if (confirmation.value) return Promise.resolve(false);

  confirmation.value = { key, parameters, destructive };
  return new Promise((resolve) => {
    resolveConfirmation = resolve;
  });
}

export function answerConfirmation(accepted) {
  const resolve = resolveConfirmation;
  resolveConfirmation = null;
  confirmation.value = null;
  resolve?.(accepted);
}
