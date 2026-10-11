<script setup>
import { t, enumLabel } from "../../common/i18n/I18n.js";
import { computed, reactive, watch } from "vue";

const props = defineProps({ models: Array, blocks: Array, busy: Boolean });
const emit = defineEmits(["verify"]);
const consent = reactive({ blockId: "", delegationId: "", modelId: "", effort: "" });
const selectedModel = computed(() =>
  props.models.find((model) => model.modelId === consent.modelId),
);

watch(
  () => props.models,
  (models) => {
    if (!models.some((model) => model.modelId === consent.modelId)) {
      consent.modelId = models[0]?.modelId ?? "";
    }
  },
  { immediate: true },
);

watch(
  () => selectedModel.value?.efforts,
  (efforts) => {
    if (!efforts?.includes(consent.effort)) {
      consent.effort = efforts?.[0] ?? "";
    }
  },
  { immediate: true },
);

watch(
  () => props.blocks,
  (blocks) => {
    if (!blocks.some((block) => block.id === consent.blockId)) {
      consent.blockId = blocks[0]?.id ?? "";
    }
  },
  { immediate: true },
);
</script>

<template>
  <details class="verification-panel">
    <summary>
      {{ t("verification.title") }}
    </summary>
    <p>
      {{ t("verification.hint") }}
    </p>
    <form @submit.prevent="emit('verify', { ...consent })">
      <fieldset
        class="resource-form-grid"
        :disabled="busy"
      >
        <div>
          <label
            for="verification-block"
            class="form-label"
          >
            {{ t("verification.block") }}
          </label>
          <select
            id="verification-block"
            v-model="consent.blockId"
            class="form-select"
            required
          >
            <option
              v-for="block in blocks"
              :key="block.id"
              :value="block.id"
            >
              {{ block.data.name }}
            </option>
          </select>
        </div>
        <div>
          <label
            for="verification-delegation"
            class="form-label"
          >
            {{ t("verification.delegation") }}
          </label>
          <input
            id="verification-delegation"
            v-model="consent.delegationId"
            class="form-control"
            required
          />
        </div>
        <div>
          <label
            for="verification-model"
            class="form-label"
          >
            {{ t("field.modelId") }}
          </label>
          <select
            id="verification-model"
            v-model="consent.modelId"
            class="form-select"
            required
          >
            <option
              v-for="model in models"
              :key="model.modelId"
              :value="model.modelId"
            >
              {{ model.modelId }}
            </option>
          </select>
        </div>
        <div>
          <label
            for="verification-effort"
            class="form-label"
          >
            {{ t("field.effort") }}
          </label>
          <select
            id="verification-effort"
            v-model="consent.effort"
            class="form-select"
            required
          >
            <option
              v-for="effort in selectedModel?.efforts ?? []"
              :key="effort"
              :value="effort"
            >
              {{ enumLabel("effort", effort) }}
            </option>
          </select>
        </div>
      </fieldset>
      <button
        type="submit"
        class="btn btn-warning mt-3"
        :disabled="busy || !consent.modelId || !consent.blockId"
      >
        {{ t("verification.authorize", { model: consent.modelId }) }}
      </button>
    </form>
  </details>
</template>
