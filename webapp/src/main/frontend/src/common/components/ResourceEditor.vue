<script setup>
import { t } from "../i18n/I18n.js";
import { reactive } from "vue";
import { formDraft, formPayload } from "../dataconverter/ResourceFormDataConverter.js";
import ResourceField from "./ResourceField.vue";

const props = defineProps({ schema: Object, record: Object, busy: Boolean });
const emit = defineEmits(["save", "cancel"]);
const draft = reactive(formDraft(props.schema.fields, props.record));
</script>

<template>
  <form
    class="card card-body editor-panel"
    @submit.prevent="emit('save', formPayload(schema.fields, draft))"
  >
    <div class="section-heading">
      <div>
        <p class="eyebrow">{{ record ? t("editor.edit") : t("editor.new") }}</p>
        <h2>{{ t(schema.label) }}</h2>
      </div>
      <span class="section-hint">
        {{ t("editor.required") }}
      </span>
    </div>
    <fieldset
      class="resource-form-grid"
      :disabled="busy"
    >
      <ResourceField
        v-for="field in schema.fields"
        :key="field.name"
        v-model="draft[field.name]"
        :field="field"
      />
    </fieldset>
    <div class="form-actions">
      <button
        type="submit"
        class="btn btn-primary"
        :disabled="busy"
      >
        {{ busy ? t("editor.saving") : t("editor.save") }}
      </button>
      <button
        type="button"
        class="btn btn-outline-secondary"
        :disabled="busy"
        @click="emit('cancel')"
      >
        {{ t("button.cancel") }}
      </button>
    </div>
  </form>
</template>
