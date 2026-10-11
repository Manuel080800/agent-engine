import { confirmAction } from "./useConfirmation.js";
import { t } from "../i18n/I18n.js";
import { computed, ref, watch } from "vue";
import { catalog } from "../config/ResourceCatalog.js";
import { resources } from "../services/resources.js";
import { session, invitation } from "../../auth/services/session.js";
import { localClients } from "../../auth/services/localClients.js";
import { tasks } from "../../task/services/tasks.js";

export function useResourceWorkspace() {
  const user = ref(null);
  const initializing = ref(true);
  const loading = ref(false);
  const saving = ref(false);
  const kind = ref("projects");
  const records = ref([]);
  const projects = ref([]);
  const project = ref("");
  const query = ref("");
  const offset = ref(0);
  const error = ref("");
  const notice = ref("");
  const editing = ref(false);
  const record = ref(null);
  const selectedTask = ref(null);
  const pageSize = 50;
  const schema = computed(() => catalog.find((item) => item.kind === kind.value));
  const busy = computed(() => loading.value || saving.value);
  let loadVersion = 0;

  async function load() {
    const version = ++loadVersion;
    loading.value = true;
    error.value = "";

    try {
      const [rows, projectRows] = await Promise.all([
        resources.list(kind.value, {
          projectId: kind.value === "projects" ? "" : project.value,
          q: query.value,
          offset: offset.value,
          limit: pageSize,
        }),
        resources.list("projects", { limit: 100 }),
      ]);

      if (version !== loadVersion) {
        return;
      }

      records.value = rows;
      projects.value = projectRows;

      if (selectedTask.value) {
        selectedTask.value = rows.find((row) => row.id === selectedTask.value.id) ?? null;
      }
    } catch (failure) {
      if (version === loadVersion) {
        error.value = failure;
      }
    } finally {
      if (version === loadVersion) {
        loading.value = false;
      }
    }
  }

  async function initialize() {
    try {
      user.value = await session();
      await load();
    } catch (failure) {
      if (failure.status !== 401) {
        error.value = failure;
      }
    } finally {
      initializing.value = false;
    }
  }

  async function run(operation) {
    if (saving.value) {
      return;
    }

    saving.value = true;
    error.value = "";
    notice.value = "";

    try {
      await operation();
      await load();
    } catch (failure) {
      error.value = failure;
    } finally {
      saving.value = false;
    }
  }

  async function edit(row = null) {
    if (row && !(await confirmAction("confirm.edit", { name: row.data.name || row.id }))) return;
    record.value = row;
    editing.value = true;
  }

  async function save(data) {
    if (
      !(await confirmAction(record.value ? "confirm.update" : "confirm.create", {
        name: data.name || t(schema.value.label),
      }))
    )
      return;
    await run(async () => {
      await resources.save(
        kind.value,
        record.value,
        data,
        kind.value === "projects" ? null : project.value,
      );
      editing.value = false;
      notice.value = { key: "notice.saved" };
    });
  }

  async function remove(row) {
    if (await confirmAction("confirm.delete", { name: row.data.name || row.id }, true)) {
      await run(() => resources.remove(kind.value, row));
    }
  }

  async function decide(row, decision) {
    if (
      !(await confirmAction(
        decision === "APPROVED" ? "confirm.approveMemory" : "confirm.rejectMemory",
        { name: row.data.name || row.id },
        decision === "REJECTED",
      ))
    )
      return;
    const editedText =
      decision === "APPROVED" ? window.prompt(t("notice.memory"), row.data.text) : null;

    if (decision === "APPROVED" && editedText === null) {
      return;
    }

    await run(() => tasks.decide(row, decision, editedText));
  }

  async function invite() {
    if (!(await confirmAction("confirm.invite"))) return;
    await run(async () => {
      const result = await invitation();
      notice.value = {
        key: "notice.invitation",
        parameters: { token: result.token },
        dateParameters: { expiresAt: result.expiresAt },
      };
    });
  }

  async function link() {
    if (!(await confirmAction("confirm.link"))) return;
    await run(() => localClients.link());
  }

  async function revoke(row) {
    if (!(await confirmAction("confirm.deactivate", { name: row.data.name || row.id }, true)))
      return;
    await run(() => localClients.revoke(row));
  }

  async function deleteClient(row) {
    if (!(await confirmAction("confirm.deleteClient", { name: row.data.name || row.id }, true)))
      return;
    await run(() => localClients.remove(row));
  }

  async function search() {
    offset.value = 0;
    await load();
  }

  async function changePage(nextOffset) {
    offset.value = Math.max(0, nextOffset);
    await load();
  }

  watch([kind, project], () => {
    offset.value = 0;
    records.value = [];
    editing.value = false;
    selectedTask.value = null;
    notice.value = "";

    if (user.value) {
      load();
    }
  });

  return {
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
  };
}
