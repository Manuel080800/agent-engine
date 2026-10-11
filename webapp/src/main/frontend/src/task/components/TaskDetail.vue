<script setup>
import {
  t,
  message,
  enumLabel,
  formatNumber,
  displayValue,
  fieldLabel,
} from "../../common/i18n/I18n.js";
import { useTaskDetail } from "../composables/useTaskDetail.js";
import ModelVerificationForm from "../../model/components/ModelVerificationForm.vue";

const props = defineProps({ task: Object });
const emit = defineEmits(["close", "changed"]);
const {
  context,
  currentTask,
  loading,
  busy,
  error,
  notice,
  verificationModels,
  canApprove,
  approve,
  verify,
} = useTaskDetail(
  () => props.task,
  () => emit("changed"),
);
</script>

<template>
  <section
    class="card card-body task-detail"
    :aria-label="t('task.detail')"
    :aria-busy="loading"
  >
    <div class="section-heading">
      <div>
        <p class="eyebrow">
          {{ t("task.review") }}
        </p>
        <h2>{{ currentTask.data.name }}</h2>
      </div>
      <div class="heading-actions">
        <span class="resource-badge">{{ enumLabel("state", currentTask.data.state) }}</span>
        <button
          type="button"
          class="btn btn-sm btn-outline-secondary"
          :disabled="busy"
          @click="emit('close')"
        >
          {{ t("button.close") }}
        </button>
      </div>
    </div>

    <div
      v-if="error"
      class="alert alert-danger"
      role="alert"
    >
      {{ message(error) }}
    </div>
    <div
      v-if="notice"
      class="alert alert-success"
      role="status"
    >
      {{ message(notice) }}
    </div>
    <p class="task-objective">{{ currentTask.data.objective }}</p>
    <p
      v-if="loading"
      role="status"
    >
      {{ t("task.loading") }}
    </p>

    <template v-if="context">
      <div class="section-heading task-plan-heading">
        <h3>
          {{ t("task.plan") }}
        </h3>
        <span
          v-if="context.plan"
          class="section-hint"
        >
          {{ t("task.version", { version: formatNumber(context.plan.version) }) }}
        </span>
      </div>
      <pre class="plan">{{ context.plan?.data.original || t("task.noPlan") }}</pre>
      <div
        v-if="context.plan"
        class="plan-summary"
      >
        <div>
          <h3>
            {{ t("task.scope") }}
          </h3>
          <ul>
            <li
              v-for="item in context.plan.data.scope"
              :key="item"
            >
              {{ item }}
            </li>
          </ul>
        </div>
        <div>
          <h3>
            {{ t("task.criteria") }}
          </h3>
          <ul>
            <li
              v-for="item in context.plan.data.acceptanceCriteria"
              :key="item"
            >
              {{ item }}
            </li>
          </ul>
        </div>
      </div>
      <details
        v-for="plan in (context.plans || []).filter((item) => item.id !== context.plan?.id)"
        :key="plan.id"
        class="card card-body mb-2"
      >
        <summary>
          {{ plan.data.name }} · {{ t("task.version", { version: formatNumber(plan.version) }) }}
        </summary>
        <pre class="plan">{{ plan.data.original }}</pre>
      </details>
      <button
        v-if="canApprove"
        type="button"
        class="btn btn-primary align-self-start"
        :disabled="busy"
        @click="approve"
      >
        {{ t("task.approve") }}
      </button>

      <h3 class="task-block-heading">
        {{ t("resource.blocks") }}
      </h3>
      <ul
        v-if="context.blocks.length"
        class="task-blocks"
      >
        <li
          v-for="block in context.blocks"
          :key="block.id"
        >
          <div>
            <strong>{{ block.data.name }}</strong>
            <small>{{ displayValue("scope", block.data.scope) }}</small>
            <small>{{ displayValue("criteria", block.data.criteria) }}</small>
          </div>
          <span class="resource-badge">{{ enumLabel("state", block.data.state) }}</span>
        </li>
      </ul>
      <p
        v-else
        class="section-hint"
      >
        {{ t("task.noBlocks") }}
      </p>
      <h3>{{ t("resource.checkpoints") }}</h3>
      <p class="section-hint">{{ t("description.checkpoints") }}</p>
      <details
        v-for="checkpoint in context.checkpoints || []"
        :key="checkpoint.id"
        class="card card-body mb-2"
      >
        <summary>
          {{ checkpoint.data.name }} · {{ enumLabel("state", checkpoint.data.state) }}
        </summary>
        <dl class="resource-data">
          <template
            v-for="(value, name) in checkpoint.data"
            :key="name"
          >
            <dt>{{ fieldLabel(name) }}</dt>
            <dd>{{ displayValue(name, value) }}</dd>
          </template>
        </dl>
      </details>
      <p
        v-if="!context.checkpoints?.length"
        class="section-hint"
      >
        {{ t("task.noCheckpoints") }}
      </p>
      <ModelVerificationForm
        v-if="verificationModels.length"
        :models="verificationModels"
        :blocks="context.blocks"
        :busy="busy"
        @verify="verify"
      />
    </template>
  </section>
</template>

<style src="../styles/task.css"></style>
