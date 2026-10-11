<script setup>
import { t, message } from "../../common/i18n/I18n.js";
import { ref } from "vue";
import { register } from "../services/session.js";

const mode = ref("redeem");
const email = ref("");
const password = ref("");
const token = ref("");
const success = ref("");
const error = ref("");
const busy = ref(false);

async function submit() {
  if (busy.value) {
    return;
  }

  busy.value = true;
  error.value = "";
  success.value = "";

  try {
    await register(mode.value, {
      email: email.value,
      password: password.value,
      token: token.value,
    });
    password.value = "";
    token.value = "";
    success.value = { key: "auth.created" };
  } catch (failure) {
    error.value = failure;
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <section class="card card-body registration-panel">
    <p class="eyebrow">
      {{ t("auth.access") }}
    </p>
    <h2>
      {{ t("auth.createTitle") }}
    </h2>
    <p class="section-hint">
      {{ t("auth.invitationHint") }}
    </p>
    <div
      v-if="error"
      class="alert alert-danger"
      role="alert"
    >
      {{ message(error) }}
    </div>
    <div
      v-if="success"
      class="alert alert-success"
      role="status"
    >
      {{ message(success) }}
    </div>

    <form @submit.prevent="submit">
      <fieldset
        class="registration-fields"
        :disabled="busy"
      >
        <div>
          <label
            for="registration-mode"
            class="form-label"
          >
            {{ t("auth.accessType") }}
          </label>
          <select
            id="registration-mode"
            v-model="mode"
            class="form-select"
          >
            <option value="redeem">
              {{ t("auth.invitation") }}
            </option>
            <option value="bootstrap">
              {{ t("auth.bootstrap") }}
            </option>
          </select>
        </div>
        <div>
          <label
            for="registration-email"
            class="form-label"
          >
            {{ t("auth.email") }}
          </label>
          <input
            id="registration-email"
            v-model="email"
            type="email"
            class="form-control"
            required
            autocomplete="email"
          />
        </div>
        <div>
          <label
            for="registration-password"
            class="form-label"
          >
            {{ t("auth.password") }}
          </label>
          <input
            id="registration-password"
            v-model="password"
            type="password"
            minlength="12"
            maxlength="100"
            class="form-control"
            required
            autocomplete="new-password"
            aria-describedby="password-hint"
          />
          <small
            id="password-hint"
            class="section-hint"
          >
            {{ t("auth.passwordHint") }}
          </small>
        </div>
        <div>
          <label
            for="registration-token"
            class="form-label"
          >
            {{ t("auth.token") }}
          </label>
          <input
            id="registration-token"
            v-model="token"
            type="password"
            class="form-control"
            required
            autocomplete="off"
          />
        </div>
      </fieldset>
      <button
        type="submit"
        class="btn btn-primary w-100 mt-4"
        :disabled="busy"
      >
        {{ busy ? t("auth.creating") : t("auth.create") }}
      </button>
    </form>
  </section>
</template>
