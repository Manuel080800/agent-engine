<script setup>
import { t, enumLabel, formatNumber, formatDate, displayValue, fieldLabel } from "../i18n/I18n.js";
defineProps({ records: Array, schema: Object, loading: Boolean, busy: Boolean });
const emit = defineEmits(["edit", "remove", "review", "decide", "revoke"]);

function preview(row) {
  return (
    row.data.objective ||
    row.data.text ||
    row.data.description ||
    row.data.modelId ||
    row.data.nextAction
  );
}

function status(row) {
  const field = ["state", "status", "scope", "role"].find(
    (key) => typeof row.data[key] === "string" && row.data[key],
  );
  return field ? enumLabel(field, row.data[field]) : t("list.personal");
}
</script>

<template>
  <section
    class="card resource-list"
    :aria-busy="loading"
    :aria-label="t('list.records')"
  >
    <div class="list-heading">
      <h2>
        {{ t("list.records") }}
      </h2>
      <span role="status">
        {{
          loading ? t("list.updating") : t("list.count", { count: formatNumber(records.length) })
        }}
      </span>
    </div>

    <div class="table-responsive">
      <table class="table align-middle mb-0">
        <thead>
          <tr>
            <th scope="col">
              {{ t("list.name") }}
            </th>
            <th scope="col">
              {{ t("list.state") }}
            </th>
            <th scope="col">
              {{ t("list.version") }}
            </th>
            <th
              scope="col"
              class="text-end"
            >
              {{ t("list.actions") }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in records"
            :key="row.id"
          >
            <td class="resource-name">
              <strong>{{ row.data.name || row.id }}</strong>
              <p
                v-if="preview(row)"
                class="row-preview"
              >
                {{ preview(row) }}
              </p>
              <small v-if="row.data.expiresAt">
                {{ t("field.expiresAt") }}: {{ formatDate(row.data.expiresAt) }}
              </small>
            </td>
            <td>
              <span class="resource-badge">{{ status(row) }}</span>
            </td>
            <td class="resource-version">{{ formatNumber(row.version) }}</td>
            <td>
              <div class="row-actions">
                <template v-if="schema.editable">
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-primary"
                    :disabled="busy"
                    @click="emit('edit', row)"
                  >
                    {{ t("button.edit") }}
                  </button>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-danger"
                    :disabled="busy"
                    @click="emit('remove', row)"
                  >
                    {{ t("button.delete") }}
                  </button>
                </template>
                <button
                  v-if="schema.kind === 'tasks'"
                  type="button"
                  class="btn btn-sm btn-outline-primary"
                  :disabled="busy"
                  @click="emit('review', row)"
                >
                  {{ t("button.review") }}
                </button>
                <template
                  v-if="schema.kind === 'memory-proposals' && row.data.status === 'PENDING'"
                >
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-success"
                    :disabled="busy"
                    @click="emit('decide', row, 'APPROVED')"
                  >
                    {{ t("button.approveEdit") }}
                  </button>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-danger"
                    :disabled="busy"
                    @click="emit('decide', row, 'REJECTED')"
                  >
                    {{ t("button.reject") }}
                  </button>
                </template>
                <button
                  v-if="schema.kind === 'local-clients' && row.data.enabled"
                  type="button"
                  class="btn btn-sm btn-outline-danger"
                  :disabled="busy"
                  @click="emit('revoke', row)"
                >
                  {{ t("button.revoke") }}
                </button>
              </div>
              <details
                v-if="!schema.editable"
                class="resource-details"
              >
                <summary>
                  {{ t("list.details") }}
                </summary>
                <dl class="resource-data">
                  <template
                    v-for="(value, name) in row.data"
                    :key="name"
                  >
                    <dt>{{ fieldLabel(name) }}</dt>
                    <dd>{{ displayValue(name, value) }}</dd>
                  </template>
                </dl>
              </details>
            </td>
          </tr>
          <tr v-if="!records.length">
            <td
              colspan="4"
              class="empty-state"
            >
              <strong>{{ loading ? t("list.loading") : t("list.empty") }}</strong>
              <p>
                {{ loading ? t("list.preparing") : t("list.emptyHint") }}
              </p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
