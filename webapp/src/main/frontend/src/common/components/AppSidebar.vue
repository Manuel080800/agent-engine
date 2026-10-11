<script setup>
import { t, locale, setLocale } from "../i18n/I18n.js";
import { computed } from "vue";
import { catalog } from "../config/ResourceCatalog.js";
import { navigationGroups } from "../config/NavigationGroups.js";

const props = defineProps({ user: Object, disabled: Boolean });
const kind = defineModel("kind", { type: String });
const emit = defineEmits(["logout"]);
const groups = computed(() =>
  navigationGroups.map((group) => ({
    label: group.label,
    items: catalog.filter((item) => group.kinds.includes(item.kind)),
  })),
);
</script>

<template>
  <aside class="sidebar">
    <a
      href="/"
      class="brand"
    >
      <span
        class="brand-mark"
        aria-hidden="true"
      >
        ae
      </span>
      <span>
        Agent Engine
        <small>
          {{ t("nav.tagline") }}
        </small>
      </span>
    </a>

    <nav
      v-if="props.user"
      class="sidebar-navigation"
      :aria-label="t('nav.modules')"
    >
      <section
        v-for="group in groups"
        :key="group.label"
        class="navigation-group"
      >
        <h2>{{ t(group.label) }}</h2>
        <button
          v-for="item in group.items"
          :key="item.kind"
          type="button"
          :class="{ active: kind === item.kind }"
          :aria-current="kind === item.kind ? 'page' : undefined"
          :disabled="props.disabled"
          @click="kind = item.kind"
        >
          {{ t(item.label) }}
        </button>
      </section>
    </nav>

    <div class="sidebar-footer">
      <label for="language-select">{{ t("language.label") }}</label>
      <select
        id="language-select"
        class="form-select mb-3"
        :value="locale"
        @change="setLocale($event.target.value)"
      >
        <option
          value="es-MX"
          lang="es"
        >
          {{ t("language.es") }}
        </option>
        <option
          value="en-US"
          lang="en"
        >
          {{ t("language.en") }}
        </option>
      </select>
      <template v-if="props.user">
        <span class="account-label">
          {{ t("nav.space") }}
        </span>
        <strong>{{ props.user.administrator ? t("nav.admin") : t("nav.personal") }}</strong>
        <button
          type="button"
          class="sidebar-logout"
          :disabled="props.disabled"
          @click="emit('logout')"
        >
          {{ t("nav.logout") }}
        </button>
      </template>
      <p v-else>
        {{ t("nav.description") }}
      </p>
    </div>
  </aside>
</template>
