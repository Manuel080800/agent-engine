<script setup>
import { ref, watch, onBeforeUnmount } from "vue";
import { t, message } from "../i18n/I18n.js";
import { confirmation, answerConfirmation } from "../composables/useConfirmation.js";

const dialog = ref(null);
watch(
  confirmation,
  (value) => {
    if (value) dialog.value.showModal();
    else dialog.value.close();
  },
  { flush: "post" },
);
onBeforeUnmount(() => answerConfirmation(false));
</script>

<template>
  <dialog
    ref="dialog"
    class="confirmation-dialog"
    aria-labelledby="confirmation-title"
    aria-describedby="confirmation-message"
    @cancel.prevent="answerConfirmation(false)"
  >
    <h2 id="confirmation-title">{{ t("confirm.title") }}</h2>
    <p id="confirmation-message">{{ message(confirmation) }}</p>
    <div class="heading-actions">
      <button
        type="button"
        class="btn btn-outline-secondary"
        autofocus
        @click="answerConfirmation(false)"
      >
        {{ t("button.cancel") }}
      </button>
      <button
        type="button"
        :class="['btn', confirmation?.destructive ? 'btn-danger' : 'btn-primary']"
        @click="answerConfirmation(true)"
      >
        {{ t("confirm.accept") }}
      </button>
    </div>
  </dialog>
</template>
