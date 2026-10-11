<script setup>
import { t, enumLabel } from "../i18n/I18n.js";
defineProps({ field: Object });
const value = defineModel();
</script>

<template>
  <div
    :class="[
      'resource-field',
      { 'field-wide': field.type === 'textarea', 'checkbox-field': field.type === 'checkbox' },
    ]"
  >
    <template v-if="field.type === 'checkbox'">
      <input
        :id="`field-${field.name}`"
        v-model="value"
        type="checkbox"
        class="form-check-input"
      />
      <label
        :for="`field-${field.name}`"
        class="form-label"
      >
        {{ t(field.label) }}
      </label>
    </template>
    <template v-else>
      <label
        :for="`field-${field.name}`"
        class="form-label"
      >
        {{ t(field.label) }}
        <span
          v-if="field.required"
          aria-hidden="true"
        >
          *
        </span>
      </label>
      <textarea
        v-if="field.type === 'textarea'"
        :id="`field-${field.name}`"
        v-model="value"
        class="form-control"
        rows="5"
        :required="field.required"
      ></textarea>
      <select
        v-else-if="field.type === 'select'"
        :id="`field-${field.name}`"
        v-model="value"
        class="form-select"
        :required="field.required"
      >
        <option
          v-for="option in field.options"
          :key="option"
          :value="option"
        >
          {{ enumLabel(field.name, option) }}
        </option>
      </select>
      <input
        v-else
        :id="`field-${field.name}`"
        v-model="value"
        :type="['number', 'datetime-local'].includes(field.type) ? field.type : 'text'"
        :step="field.type === 'datetime-local' ? '0.001' : undefined"
        class="form-control"
        :required="field.required"
      />
    </template>
  </div>
</template>
