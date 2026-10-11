<script setup>
import { onMounted, ref } from "vue";
import { t, message } from "../../common/i18n/I18n.js";
import { request } from "../../common/services/http.js";

const csrf = ref(null);
const busy = ref(false);
const parameters = new URLSearchParams(location.search);
const error = ref(
  parameters.has("error")
    ? { key: "login.failed" }
    : parameters.has("expired")
      ? { key: "login.expired" }
      : "",
);

onMounted(async () => {
  try {
    csrf.value = await request("/auth/csrf");
  } catch (failure) {
    error.value = failure;
  }
});

function submit(event) {
  if (busy.value || !csrf.value) {
    event.preventDefault();
    return;
  }
  busy.value = true;
}
</script>

<template>
  <section class="card card-body registration-panel login-panel mx-auto">
    <p class="eyebrow">{{ t("auth.access") }}</p>
    <h1>{{ t("auth.login") }}</h1>
    <div
      v-if="error"
      class="alert alert-danger"
      role="alert"
    >
      {{ message(error) }}
    </div>
    <form
      method="post"
      action="/login"
      @submit="submit"
    >
      <input
        v-if="csrf"
        type="hidden"
        :name="csrf.parameterName"
        :value="csrf.token"
      />
      <fieldset
        class="registration-fields"
        :disabled="busy || !csrf"
      >
        <div>
          <label
            for="login-email"
            class="form-label"
          >
            {{ t("auth.email") }}
          </label>
          <input
            id="login-email"
            name="username"
            type="email"
            autocomplete="username"
            required
            class="form-control"
          />
        </div>
        <div>
          <label
            for="login-password"
            class="form-label"
          >
            {{ t("auth.password") }}
          </label>
          <input
            id="login-password"
            name="password"
            type="password"
            autocomplete="current-password"
            required
            class="form-control"
          />
        </div>
      </fieldset>
      <button
        type="submit"
        class="btn btn-primary w-100 mt-4"
        :disabled="busy || !csrf"
      >
        {{ t(busy ? "login.loading" : "auth.login") }}
      </button>
    </form>
    <a
      class="mt-3"
      href="/"
    >
      {{ t("login.back") }}
    </a>
  </section>
</template>

<style src="../styles/auth.css"></style>
