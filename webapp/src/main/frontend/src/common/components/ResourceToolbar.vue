<script setup>
import { t } from "../i18n/I18n.js";
defineProps({ projects: Array, busy: Boolean, administrator: Boolean });
const project = defineModel("project", { type: String });
const query = defineModel("query", { type: String });
const emit = defineEmits(["search", "invite"]);
</script>

<template>
  <div class="toolbar">
    <label
      class="project-filter"
      for="project-filter"
    >
      <span class="filter-label">
        {{ t("filter.project") }}
      </span>
      <select
        id="project-filter"
        v-model="project"
        class="form-select"
        :disabled="busy"
      >
        <option value="">
          {{ t("filter.allProjects") }}
        </option>
        <option
          v-for="item in projects"
          :key="item.id"
          :value="item.id"
        >
          {{ item.data.name }}
        </option>
      </select>
    </label>

    <form
      class="search-filter"
      role="search"
      @submit.prevent="emit('search')"
    >
      <label
        class="filter-label"
        for="resource-search"
      >
        {{ t("filter.content") }}
      </label>
      <div class="search-input">
        <input
          id="resource-search"
          v-model="query"
          type="search"
          class="form-control"
          :placeholder="t('filter.placeholder')"
          :disabled="busy"
        />
        <button
          type="submit"
          class="btn btn-outline-secondary"
          :disabled="busy"
        >
          {{ t("filter.search") }}
        </button>
      </div>
    </form>

    <button
      v-if="administrator"
      type="button"
      class="btn btn-outline-secondary invitation-button"
      :disabled="busy"
      :title="t('help.invitation')"
      @click="emit('invite')"
    >
      {{ t("filter.invite") }}
    </button>
  </div>
</template>
