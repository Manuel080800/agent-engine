<script setup>
import { t, formatNumber } from "../i18n/I18n.js";
defineProps({ offset: Number, count: Number, pageSize: Number, busy: Boolean });
const emit = defineEmits(["change"]);
</script>

<template>
  <nav
    class="pagination-bar"
    :aria-label="t('pagination.label')"
  >
    <button
      type="button"
      class="btn btn-outline-secondary"
      :disabled="offset === 0 || busy"
      @click="emit('change', Math.max(0, offset - pageSize))"
    >
      {{ t("pagination.previous") }}
    </button>
    <span>
      {{ t("pagination.page", { page: formatNumber(Math.floor(offset / pageSize) + 1) }) }}
    </span>
    <button
      type="button"
      class="btn btn-outline-secondary"
      :disabled="count < pageSize || busy"
      @click="emit('change', offset + pageSize)"
    >
      {{ t("pagination.next") }}
    </button>
  </nav>
</template>
