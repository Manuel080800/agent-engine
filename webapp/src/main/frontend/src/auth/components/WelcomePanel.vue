<script setup>
import { t } from "../../common/i18n/I18n.js";
import RegistrationPanel from "./RegistrationPanel.vue";
</script>

<template>
  <div class="welcome-layout">
    <section class="welcome-heading">
      <p class="eyebrow">
        {{ t("welcome.workspace") }}
      </p>
      <h1>
        {{ t("welcome.plans") }}
        <br />
        {{ t("welcome.work") }}
      </h1>
      <p class="welcome-description">
        {{ t("welcome.description") }}
      </p>
      <a
        href="/login"
        class="btn btn-primary"
      >
        {{ t("auth.login") }}
      </a>
      <div class="welcome-features">
        <div>
          <span>01</span>
          <strong>
            {{ t("welcome.context") }}
          </strong>
          <p>
            {{ t("welcome.contextHint") }}
          </p>
        </div>
        <div>
          <span>02</span>
          <strong>
            {{ t("welcome.decisions") }}
          </strong>
          <p>
            {{ t("welcome.decisionsHint") }}
          </p>
        </div>
        <div>
          <span>03</span>
          <strong>
            {{ t("welcome.progress") }}
          </strong>
          <p>
            {{ t("welcome.progressHint") }}
          </p>
        </div>
      </div>
    </section>
    <RegistrationPanel />
  </div>
</template>

<style src="../styles/auth.css"></style>
