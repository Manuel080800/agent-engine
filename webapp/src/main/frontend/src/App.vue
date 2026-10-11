<script setup>
import { t, message } from "./common/i18n/I18n.js";
import ConfirmationDialog from "./common/components/ConfirmationDialog.vue";
import { confirmAction } from "./common/composables/useConfirmation.js";
import { onMounted } from "vue";
import { logout } from "./auth/services/session.js";
import { useResourceWorkspace } from "./common/composables/useResourceWorkspace.js";
import AppSidebar from "./common/components/AppSidebar.vue";
import ResourceToolbar from "./common/components/ResourceToolbar.vue";
import ResourceTable from "./common/components/ResourceTable.vue";
import ResourcePagination from "./common/components/ResourcePagination.vue";
import ResourceEditor from "./common/components/ResourceEditor.vue";
import TaskDetail from "./task/components/TaskDetail.vue";
import LoginPanel from "./auth/components/LoginPanel.vue";
import WelcomePanel from "./auth/components/WelcomePanel.vue";

const {
  user,
  initializing,
  loading,
  saving,
  busy,
  kind,
  records,
  projects,
  project,
  query,
  offset,
  error,
  notice,
  editing,
  record,
  selectedTask,
  pageSize,
  schema,
  initialize,
  load,
  edit,
  save,
  remove,
  decide,
  invite,
  link,
  revoke,
  deleteClient,
  search,
  changePage,
} = useResourceWorkspace();

async function signOut() {
  if (!(await confirmAction("confirm.logout"))) return;
  try {
    await logout();
  } catch (failure) {
    error.value = failure;
  }
}

const loginPage = location.pathname === "/login";

if (loginPage) {
  initializing.value = false;
} else {
  onMounted(initialize);
}
</script>

<template>
  <div class="app-shell">
    <ConfirmationDialog />
    <a
      class="skip-link"
      href="#main-content"
    >
      {{ t("app.skip") }}
    </a>
    <AppSidebar
      v-model:kind="kind"
      :user="user"
      :disabled="saving"
      @logout="signOut"
    />

    <main
      id="main-content"
      class="main-content"
    >
      <div
        v-if="initializing"
        class="page-loading"
        role="status"
      >
        <span
          class="spinner-border spinner-border-sm"
          aria-hidden="true"
        ></span>
        {{ t("app.loading") }}
      </div>

      <template v-else>
        <div
          v-if="error"
          class="alert alert-danger"
          role="alert"
        >
          {{ message(error) }}
        </div>
        <LoginPanel v-if="loginPage" />
        <WelcomePanel v-else-if="!user" />

        <template v-else>
          <header class="page-heading">
            <div>
              <p class="eyebrow">
                {{ t("app.workspace") }}
              </p>
              <h1>{{ t(schema.label) }}</h1>
              <p class="page-description">
                {{ t(`description.${kind}`) }}
              </p>
            </div>
            <div class="heading-actions">
              <button
                v-if="!editing && !selectedTask && (schema.editable || schema.creatable)"
                type="button"
                class="btn btn-primary"
                :disabled="busy"
                @click="edit()"
              >
                {{ t("app.create") }}
              </button>
              <button
                v-if="kind === 'local-clients'"
                type="button"
                class="btn btn-primary"
                :disabled="busy"
                @click="link"
              >
                {{ t("app.link") }}
              </button>
            </div>
          </header>

          <div
            v-if="notice"
            class="alert alert-info workspace-notice"
            role="status"
          >
            <span>{{ message(notice) }}</span>
            <button
              type="button"
              class="btn-close"
              :aria-label="t('app.dismiss')"
              @click="notice = ''"
            ></button>
          </div>

          <ResourceToolbar
            v-if="!editing && !selectedTask"
            v-model:project="project"
            v-model:query="query"
            :projects="projects"
            :busy="busy"
            :administrator="user.administrator && kind === 'local-clients'"
            @search="search"
            @invite="invite"
          />
          <ResourceEditor
            v-if="editing"
            :key="record?.id || kind"
            :schema="schema"
            :record="record"
            :busy="saving"
            @save="save"
            @cancel="editing = false"
          />
          <TaskDetail
            v-if="selectedTask"
            :key="selectedTask.id"
            :task="selectedTask"
            @close="selectedTask = null"
            @changed="load"
          />
          <ResourceTable
            v-if="!editing && !selectedTask"
            :records="records"
            :schema="schema"
            :loading="loading"
            :busy="busy"
            @edit="edit"
            @remove="remove"
            @review="selectedTask = $event"
            @decide="decide"
            @revoke="revoke"
            @delete-client="deleteClient"
          />
          <ResourcePagination
            v-if="!editing && !selectedTask"
            :offset="offset"
            :count="records.length"
            :page-size="pageSize"
            :busy="busy"
            @change="changePage"
          />
        </template>
      </template>
    </main>
  </div>
</template>
