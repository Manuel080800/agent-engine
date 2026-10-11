import { confirmAction } from "../../common/composables/useConfirmation.js";
import { t } from "../../common/i18n/I18n.js";
import { computed, ref, watch } from "vue";
import { tasks } from "../services/tasks.js";

export function useTaskDetail(task, changed) {
  const context = ref(null);
  const loading = ref(false);
  const busy = ref(false);
  const error = ref("");
  const notice = ref("");
  const currentTask = computed(() => context.value?.task ?? task());
  const verificationModels = computed(() =>
    (context.value?.models ?? []).filter((model) => model.requiresVerification),
  );
  const canApprove = computed(() =>
    ["PLANNED", "WAITING_APPROVAL", "BLOCKED"].includes(currentTask.value.data.state),
  );
  let loadVersion = 0;

  async function load() {
    const version = ++loadVersion;
    loading.value = true;

    try {
      const result = await tasks.context(task());

      if (version === loadVersion) {
        context.value = result;
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

  async function action(operation) {
    if (busy.value) {
      return;
    }

    busy.value = true;
    error.value = "";
    notice.value = "";

    try {
      const result = await operation();
      notice.value = { key: "notice.operation", parameters: { id: result.id } };
      await load();
      changed();
    } catch (failure) {
      error.value = failure;
    } finally {
      busy.value = false;
    }
  }

  async function approve() {
    if (!(await confirmAction("confirm.approvePlan", { name: currentTask.value.data.name })))
      return;
    return action(() => tasks.approve(currentTask.value));
  }

  async function verify(data) {
    if (!(await confirmAction("confirm.verify", { model: data.modelId }))) return;
    return action(() => tasks.consent(currentTask.value, data));
  }

  watch(
    () => [task().id, task().version],
    () => {
      context.value = null;
      error.value = "";
      load();
    },
    { immediate: true },
  );

  return {
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
  };
}
