<template>
  <div class="workspace">
    <div class="backdrop backdrop-one"></div>
    <div class="backdrop backdrop-two"></div>

    <header class="topbar panel-surface">
      <div class="brand-block">
        <div class="brand-mark">DM</div>
        <div class="brand-copy">
          <div class="brand-kicker">
            <span>{{ t("app.brand") }}</span>
            <span class="brand-badge">{{ t("app.defaultBadge") }}</span>
          </div>
          <h1>{{ t("app.title") }}</h1>
          <p>{{ t("app.subtitle") }}</p>
        </div>
      </div>

      <div class="topbar-actions">
        <div class="sync-pill">{{ lastSyncedLabel }}</div>
        <div class="locale-select-wrap">
          <n-select
            :value="locale.value"
            size="small"
            :options="localeOptions"
            :aria-label="t('app.localeLabel')"
            @update:value="setLocale"
          />
        </div>
      </div>
    </header>

    <div class="content-grid">
      <aside class="sidebar-column">
        <section class="sidebar-intro panel-surface">
          <div class="section-heading compact">
            <div>
              <span class="eyebrow">{{ t("overview.title") }}</span>
              <h2>{{ t("overview.subtitle") }}</h2>
            </div>
          </div>

          <div class="metric-grid">
            <article class="metric-card accent-blue">
              <span>{{ t("overview.hostCount") }}</span>
              <strong>{{ store.hosts.value.length }}</strong>
            </article>
            <article class="metric-card accent-emerald">
              <span>{{ t("overview.containerCount") }}</span>
              <strong>{{ store.containers.value.length }}</strong>
            </article>
            <article class="metric-card accent-amber">
              <span>{{ t("overview.runningCount") }}</span>
              <strong>{{ runningContainersCount }}</strong>
            </article>
            <article class="metric-card accent-rose">
              <span>{{ t("overview.imageCount") }}</span>
              <strong>{{ uniqueImagesCount }}</strong>
            </article>
          </div>
        </section>

        <n-card :bordered="false" class="panel-card form-panel">
          <div class="section-heading">
            <div>
              <span class="eyebrow">SSH</span>
              <h2>{{ t("host.formTitle") }}</h2>
              <p>{{ t("host.formSubtitle") }}</p>
            </div>
          </div>

          <n-form label-placement="top" :model="hostForm">
            <n-form-item :label="t('host.fields.name')">
              <n-input v-model:value="hostForm.name" :placeholder="t('host.placeholders.name')" />
            </n-form-item>

            <n-form-item :label="t('host.fields.address')">
              <n-input
                v-model:value="hostForm.address"
                :placeholder="t('host.placeholders.address')"
              />
            </n-form-item>

            <div class="form-row two-columns">
              <n-form-item :label="t('host.fields.port')">
                <n-input-number
                  v-model:value="hostForm.port"
                  :min="1"
                  :max="65535"
                  :show-button="false"
                />
              </n-form-item>

              <n-form-item :label="t('host.fields.username')">
                <n-input
                  v-model:value="hostForm.username"
                  :placeholder="t('host.placeholders.username')"
                />
              </n-form-item>
            </div>

            <n-form-item :label="t('host.fields.authType')">
              <n-select v-model:value="hostForm.authType" :options="authTypeOptions" />
            </n-form-item>

            <n-form-item v-if="hostForm.authType === 'PASSWORD'" :label="t('host.fields.password')">
              <n-input
                v-model:value="hostForm.password"
                type="password"
                show-password-on="click"
                :placeholder="t('host.placeholders.password')"
              />
            </n-form-item>

            <template v-else>
              <n-form-item :label="t('host.fields.privateKeyPath')">
                <n-input
                  v-model:value="hostForm.privateKeyPath"
                  :placeholder="t('host.placeholders.privateKeyPath')"
                />
              </n-form-item>

              <n-form-item :label="t('host.fields.privateKeyPassphrase')">
                <n-input
                  v-model:value="hostForm.privateKeyPassphrase"
                  type="password"
                  show-password-on="click"
                  :placeholder="t('host.placeholders.privateKeyPassphrase')"
                />
              </n-form-item>
            </template>
          </n-form>

          <div class="form-helper">{{ t("host.helper") }}</div>

          <n-space class="action-row" justify="space-between">
            <n-button type="primary" strong :disabled="!canSubmitHost" @click="createHost">
              {{ t("actions.addHost") }}
            </n-button>
            <n-button secondary :loading="store.loading.value" @click="refreshAll">
              {{ t("actions.refresh") }}
            </n-button>
          </n-space>
        </n-card>

        <n-card :bordered="false" class="panel-card host-list-panel">
          <div class="section-heading">
            <div>
              <span class="eyebrow">HOSTS</span>
              <h2>{{ t("host.listTitle") }}</h2>
              <p>{{ t("host.listSubtitle") }}</p>
            </div>
          </div>

          <n-scrollbar style="max-height: 420px">
            <div v-if="store.hosts.value.length > 0" class="host-list">
              <article
                v-for="host in store.hosts.value"
                :key="host.id"
                :class="['host-tile', { active: store.selectedHostId.value === host.id }]"
              >
                <button type="button" class="host-tile-main" @click="selectHost(host.id)">
                  <div class="host-tile-top">
                    <strong>{{ host.name }}</strong>
                    <n-tag size="small" :type="healthTagType(host.id)" round>
                      {{ hostHealthLabel(host.id) }}
                    </n-tag>
                  </div>
                  <p>{{ host.username }}@{{ host.address }}:{{ host.port }}</p>
                  <div class="host-tile-meta">
                    <span>{{ authTypeLabel(host.authType) }}</span>
                    <span v-if="store.selectedHostId.value === host.id">{{
                      t("host.selected")
                    }}</span>
                  </div>
                </button>

                <n-popconfirm @positive-click="deleteHost(host.id)">
                  <template #trigger>
                    <n-button text type="error" @click.stop>
                      {{ t("actions.deleteHost") }}
                    </n-button>
                  </template>
                  {{ t("host.deleteConfirm") }}
                </n-popconfirm>
              </article>
            </div>

            <div v-else class="host-empty-state">
              <n-empty :title="t('host.emptyTitle')" :description="t('host.emptyDescription')" />
            </div>
          </n-scrollbar>
        </n-card>
      </aside>

      <main class="main-column">
        <section class="table-panel panel-surface priority-panel">
          <div class="section-heading split table-heading compact-heading">
            <div>
              <span class="eyebrow">DOCKER</span>
              <h2>{{ t("container.title") }}</h2>
              <p>{{ t("container.subtitle") }}</p>
            </div>

            <div class="panel-heading-right">
              <div class="host-chip">{{ selectedHostTitle }}</div>
              <div class="results-pill">
                {{ filteredContainers.length }} / {{ store.containers.value.length }}
              </div>
            </div>
          </div>

          <div class="toolbar-card compact-toolbar">
            <div>
              <h3>{{ t("container.filtersTitle") }}</h3>
              <p>{{ t("container.filtersSubtitle") }}</p>
            </div>

            <div class="toolbar-controls">
              <n-input
                v-model:value="searchQuery"
                clearable
                :placeholder="t('container.searchPlaceholder')"
              />
              <n-select
                v-model:value="stateFilter"
                :options="stateOptions"
                :placeholder="t('container.stateFilterPlaceholder')"
              />
              <n-button secondary :disabled="!store.selectedHostId.value" @click="loadContainers">
                {{ t("actions.refreshData") }}
              </n-button>
            </div>
          </div>

          <div v-if="!store.selectedHostId.value" class="empty-panel">
            <n-empty
              :title="t('container.noHostTitle')"
              :description="t('container.noHostDescription')"
            />
          </div>

          <div
            v-else-if="filteredContainers.length === 0 && !store.loading.value"
            class="empty-panel"
          >
            <n-empty
              :title="t('container.emptyTitle')"
              :description="t('container.emptyDescription')"
            />
          </div>

          <div v-else class="data-table-shell data-table-shell-large">
            <n-data-table
              :columns="columns"
              :data="filteredContainers"
              :loading="store.loading.value"
              :row-key="(row) => row.id"
              :max-height="640"
              striped
            />
          </div>
        </section>

        <section class="hero-panel panel-surface compact-hero">
          <div class="section-heading split compact-heading">
            <div>
              <span class="eyebrow">{{ t("overview.selectedHost") }}</span>
              <h2>{{ selectedHostTitle }}</h2>
              <p>{{ selectedHostSummary }}</p>
            </div>

            <n-space>
              <n-button
                secondary
                size="small"
                :disabled="!store.selectedHostId.value"
                @click="testConnection"
              >
                {{ t("actions.testConnection") }}
              </n-button>
              <n-button
                type="primary"
                size="small"
                :disabled="!store.selectedHostId.value"
                :loading="store.loading.value"
                @click="loadContainers"
              >
                {{ t("actions.loadContainers") }}
              </n-button>
            </n-space>
          </div>

          <div class="selected-host-grid compact-host-grid">
            <article class="host-fact-card compact-card">
              <span>{{ t("overview.connection") }}</span>
              <strong>{{ selectedHostHealthText }}</strong>
              <small>{{ selectedHostCheckedLabel }}</small>
            </article>
            <article class="host-fact-card compact-card">
              <span>{{ t("overview.authType") }}</span>
              <strong>{{ selectedHostAuthText }}</strong>
              <small>{{ selectedHostPortText }}</small>
            </article>
            <article class="host-fact-card compact-card">
              <span>{{ t("overview.containerCount") }}</span>
              <strong>{{ store.containers.value.length }}</strong>
              <small>{{ t("overview.runningCount") }} {{ runningContainersCount }}</small>
            </article>
          </div>
        </section>
      </main>
    </div>

    <n-modal v-model:show="execVisible">
      <div class="terminal-modal">
        <div class="terminal-modal-header">
          <div>
            <span class="eyebrow">TERMINAL</span>
            <h3>{{ t("container.execTitle") }}</h3>
            <p>{{ activeExecContainer?.names ?? selectedHostTitle }}</p>
          </div>
          <n-button text @click="execVisible = false">{{ t("actions.close") }}</n-button>
        </div>

        <div class="terminal-box">{{ execOutput || t("container.execEmpty") }}</div>

        <div class="terminal-input-row">
          <n-input
            v-model:value="execInput"
            :placeholder="t('container.execPlaceholder')"
            @keyup.enter="sendExec"
          />
          <n-button type="primary" @click="sendExec">{{ t("actions.send") }}</n-button>
        </div>
      </div>
    </n-modal>

    <n-modal v-model:show="logsVisible">
      <div class="terminal-modal">
        <div class="terminal-modal-header">
          <div>
            <span class="eyebrow">LOGS</span>
            <h3>{{ t("container.logsTitle") }}</h3>
            <p>{{ activeLogsContainer?.names ?? selectedHostTitle }}</p>
          </div>
          <n-button text @click="logsVisible = false">{{ t("actions.close") }}</n-button>
        </div>

        <div class="terminal-box">{{ logsOutput || t("container.logsEmpty") }}</div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import {
  NButton,
  NPopconfirm,
  NSpace,
  NTag,
  createDiscreteApi,
  type DataTableColumns,
  type SelectOption,
} from "naive-ui";
import { api } from "./api/client";
import { useI18n } from "./i18n";
import { useDockerStore } from "./stores/useDockerStore";
import type { ContainerSummary, CreateHostPayload, SshAuthType } from "./types";

type HostFormModel = Omit<CreateHostPayload, "port"> & {
  port: number | null;
  password: string;
  privateKeyPath: string;
  privateKeyPassphrase: string;
};

type HostHealthState = "idle" | "success" | "error";

const ALL_STATES = "__ALL__";

const { locale, setLocale, t } = useI18n();
const store = useDockerStore();
const { message } = createDiscreteApi(["message"]);

const hostForm = reactive<HostFormModel>({
  name: "",
  address: "",
  port: 22,
  username: "",
  authType: "PASSWORD",
  password: "",
  privateKeyPath: "",
  privateKeyPassphrase: "",
});

const localeOptions = computed(() => [
  { label: t("language.zhCN"), value: "zh-CN" as const },
  { label: t("language.enUS"), value: "en-US" as const },
]);

const authTypeOptions = computed<SelectOption[]>(() => [
  { label: t("host.auth.PASSWORD"), value: "PASSWORD" },
  { label: t("host.auth.PRIVATE_KEY"), value: "PRIVATE_KEY" },
]);

const searchQuery = ref("");
const stateFilter = ref<string>(ALL_STATES);
const execVisible = ref(false);
const logsVisible = ref(false);
const execInput = ref("");
const execOutput = ref("");
const logsOutput = ref("");
const lastSyncedAt = ref<Date | null>(null);
const activeExecContainer = ref<ContainerSummary | null>(null);
const activeLogsContainer = ref<ContainerSummary | null>(null);
const hostHealth = ref<Record<string, HostHealthState>>({});
const hostCheckedAt = ref<Record<string, string>>({});
let execSocket: WebSocket | null = null;
let logsSocket: WebSocket | null = null;

const filteredContainers = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase();

  return store.containers.value.filter((container) => {
    const matchesQuery =
      keyword.length === 0 ||
      [container.id, container.names, container.image, container.state, container.status]
        .join(" ")
        .toLowerCase()
        .includes(keyword);

    const matchesState =
      stateFilter.value === ALL_STATES || normalizeState(container.state) === stateFilter.value;

    return matchesQuery && matchesState;
  });
});

const stateOptions = computed<SelectOption[]>(() => {
  const uniqueStates = Array.from(
    new Set(store.containers.value.map((container) => normalizeState(container.state))),
  ).filter(Boolean);

  return [
    { label: t("container.allStates"), value: ALL_STATES },
    ...uniqueStates.map((state) => ({
      label: translateState(state),
      value: state,
    })),
  ];
});

const runningContainersCount = computed(
  () =>
    store.containers.value.filter((container) => normalizeState(container.state) === "running")
      .length,
);

const uniqueImagesCount = computed(
  () => new Set(store.containers.value.map((container) => container.image)).size,
);

const canSubmitHost = computed(() => {
  const hasBaseFields =
    hostForm.name.trim().length > 0 &&
    hostForm.address.trim().length > 0 &&
    hostForm.username.trim().length > 0 &&
    !!hostForm.port;

  if (!hasBaseFields) {
    return false;
  }

  if (hostForm.authType === "PASSWORD") {
    return hostForm.password.length > 0;
  }

  return hostForm.privateKeyPath.trim().length > 0;
});

const lastSyncedLabel = computed(() => {
  if (!lastSyncedAt.value) {
    return t("app.neverSynced");
  }

  return t("app.lastSync", { time: formatDateTime(lastSyncedAt.value) });
});

const selectedHostTitle = computed(() => store.selectedHost.value?.name ?? t("overview.noHost"));

const selectedHostSummary = computed(() => {
  const host = store.selectedHost.value;

  if (!host) {
    return t("container.noHostDescription");
  }

  return `${host.username}@${host.address}:${host.port}`;
});

const selectedHostHealthText = computed(() => {
  const host = store.selectedHost.value;
  if (!host) {
    return t("overview.untested");
  }

  return hostHealthLabel(host.id);
});

const selectedHostCheckedLabel = computed(() => {
  const host = store.selectedHost.value;
  if (!host) {
    return t("overview.notChecked");
  }

  const checkedAt = hostCheckedAt.value[host.id];
  if (!checkedAt) {
    return t("overview.notChecked");
  }

  return `${t("overview.lastChecked")} ${checkedAt}`;
});

const selectedHostAuthText = computed(() => {
  const host = store.selectedHost.value;
  return host ? authTypeLabel(host.authType) : "--";
});

const selectedHostPortText = computed(() => {
  const host = store.selectedHost.value;
  return host ? `${t("host.fields.port")} ${host.port}` : "--";
});

const columns = computed<DataTableColumns<ContainerSummary>>(() => [
  {
    title: t("container.columns.id"),
    key: "id",
    width: 130,
    ellipsis: { tooltip: true },
    render: (row) => h("code", { class: "table-id" }, shortId(row.id)),
  },
  {
    title: t("container.columns.name"),
    key: "names",
    minWidth: 160,
    ellipsis: { tooltip: true },
    render: (row) =>
      h("div", { class: "table-stack" }, [
        h("strong", { class: "table-primary" }, row.names),
        h("span", { class: "table-secondary" }, row.status),
      ]),
  },
  {
    title: t("container.columns.image"),
    key: "image",
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: (row) => h("span", { class: "table-image" }, row.image),
  },
  {
    title: t("container.columns.state"),
    key: "state",
    width: 120,
    render: (row) =>
      h(
        NTag,
        {
          type: stateTagType(row.state),
          round: true,
          size: "small",
        },
        { default: () => translateState(row.state) },
      ),
  },
  {
    title: t("container.columns.status"),
    key: "status",
    minWidth: 180,
    ellipsis: { tooltip: true },
  },
  {
    title: t("container.columns.actions"),
    key: "actions",
    width: 360,
    render: (row) =>
      h(
        NSpace,
        { wrap: true, size: [8, 8] },
        {
          default: () => [
            h(
              NButton,
              {
                size: "small",
                type: "primary",
                disabled: normalizeState(row.state) === "running",
                onClick: () => void startContainer(row),
              },
              { default: () => t("actions.start") },
            ),
            h(
              NButton,
              {
                size: "small",
                secondary: true,
                disabled: !isContainerRunning(row),
                onClick: () => openExec(row),
              },
              { default: () => t("actions.exec") },
            ),
            h(
              NButton,
              { size: "small", secondary: true, onClick: () => openLogs(row) },
              { default: () => t("actions.logs") },
            ),
            h(
              NPopconfirm,
              { onPositiveClick: () => void removeContainer(row, false) },
              {
                trigger: () =>
                  h(
                    NButton,
                    { size: "small", secondary: true },
                    { default: () => t("actions.remove") },
                  ),
                default: () => t("container.removeConfirm"),
              },
            ),
            h(
              NPopconfirm,
              { onPositiveClick: () => void removeContainer(row, true) },
              {
                trigger: () =>
                  h(
                    NButton,
                    { size: "small", type: "error", secondary: true },
                    { default: () => t("actions.deepRemove") },
                  ),
                default: () => t("container.deepRemoveConfirm"),
              },
            ),
          ],
        },
      ),
  },
]);

onMounted(async () => {
  await refreshAll(false);
});

onBeforeUnmount(() => {
  closeExec();
  closeLogs();
});

watch(
  () => store.selectedHostId.value,
  async () => {
    closeExec();
    closeLogs();
    await store.refreshContainers();
    stateFilter.value = ALL_STATES;
    searchQuery.value = "";
    lastSyncedAt.value = new Date();
  },
);

watch(
  () => hostForm.authType,
  (authType) => {
    if (authType === "PASSWORD") {
      hostForm.privateKeyPath = "";
      hostForm.privateKeyPassphrase = "";
      return;
    }

    hostForm.password = "";
  },
);

watch(execVisible, (visible) => {
  if (!visible) {
    closeExec();
  }
});

watch(logsVisible, (visible) => {
  if (!visible) {
    closeLogs();
  }
});

function authTypeLabel(authType: SshAuthType) {
  return t(`host.auth.${authType}`);
}

function normalizeState(state: string) {
  return state.trim().toLowerCase();
}

function translateState(state: string) {
  const mapping: Record<string, string> = {
    running: "status.running",
    exited: "status.exited",
    created: "status.created",
    paused: "status.paused",
    restarting: "status.restarting",
    dead: "status.dead",
    removing: "status.removing",
  };

  return t(mapping[normalizeState(state)] ?? "status.unknown");
}
function stateTagType(state: string) {
  const normalized = normalizeState(state);
  if (normalized === "running") return "success" as const;
  if (normalized === "paused" || normalized === "restarting") return "warning" as const;
  if (normalized === "dead" || normalized === "removing") return "error" as const;
  return "default" as const;
}

function healthTagType(hostId: string) {
  const health = hostHealth.value[hostId] ?? "idle";
  if (health === "success") return "success" as const;
  if (health === "error") return "error" as const;
  return "default" as const;
}

function hostHealthLabel(hostId: string) {
  const health = hostHealth.value[hostId] ?? "idle";
  if (health === "success") return t("overview.healthy");
  if (health === "error") return t("overview.unhealthy");
  return t("overview.untested");
}

function formatDateTime(value: Date | string) {
  const date = typeof value === "string" ? new Date(value) : value;
  return new Intl.DateTimeFormat(locale.value, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function shortId(value: string) {
  return value.slice(0, 12);
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }

  return t("messages.requestFailed");
}

function markSynced() {
  lastSyncedAt.value = new Date();
}

function resetHostForm() {
  hostForm.name = "";
  hostForm.address = "";
  hostForm.port = 22;
  hostForm.username = "";
  hostForm.authType = "PASSWORD";
  hostForm.password = "";
  hostForm.privateKeyPath = "";
  hostForm.privateKeyPassphrase = "";
}

function buildHostPayload(): CreateHostPayload {
  return {
    name: hostForm.name.trim(),
    address: hostForm.address.trim(),
    port: Number(hostForm.port ?? 22),
    username: hostForm.username.trim(),
    authType: hostForm.authType,
    password: hostForm.authType === "PASSWORD" ? hostForm.password : undefined,
    privateKeyPath:
      hostForm.authType === "PRIVATE_KEY" ? hostForm.privateKeyPath.trim() : undefined,
    privateKeyPassphrase:
      hostForm.authType === "PRIVATE_KEY" && hostForm.privateKeyPassphrase.length > 0
        ? hostForm.privateKeyPassphrase
        : undefined,
  };
}

function selectHost(hostId: string) {
  store.selectedHostId.value = hostId;
}

async function createHost() {
  if (!canSubmitHost.value) {
    message.warning(t("messages.hostFormIncomplete"));
    return;
  }

  try {
    await store.addHost(buildHostPayload());
    resetHostForm();
    markSynced();
    await store.refreshContainers();
    message.success(t("messages.hostAdded"));
  } catch (error) {
    message.error(getErrorMessage(error));
  }
}

async function deleteHost(hostId: string) {
  try {
    await store.removeHost(hostId);
    delete hostHealth.value[hostId];
    delete hostCheckedAt.value[hostId];
    await store.refreshContainers();
    markSynced();
    message.success(t("messages.hostDeleted"));
  } catch (error) {
    message.error(getErrorMessage(error));
  }
}

async function refreshAll(showSuccess = true) {
  try {
    await store.reloadHosts();
    await store.refreshContainers();
    markSynced();

    if (showSuccess) {
      message.success(t("messages.refreshed"));
    }
  } catch (error) {
    message.error(getErrorMessage(error));
  }
}

async function testConnection() {
  if (!store.selectedHostId.value) return;

  try {
    await api.testHost(store.selectedHostId.value);
    hostHealth.value[store.selectedHostId.value] = "success";
    hostCheckedAt.value[store.selectedHostId.value] = formatDateTime(new Date());
    message.success(t("messages.connectionPassed"));
  } catch (error) {
    hostHealth.value[store.selectedHostId.value] = "error";
    hostCheckedAt.value[store.selectedHostId.value] = formatDateTime(new Date());
    message.error(getErrorMessage(error));
  }
}

async function loadContainers() {
  try {
    await store.refreshContainers();
    markSynced();
  } catch (error) {
    message.error(getErrorMessage(error));
  }
}

async function startContainer(row: ContainerSummary) {
  if (!store.selectedHostId.value) return;

  try {
    await api.startContainer(store.selectedHostId.value, row.id);
    message.success(t("messages.containerStarted", { name: row.names }));
    await store.refreshContainers();
    markSynced();
  } catch (error) {
    message.error(getErrorMessage(error));
  }
}

async function removeContainer(row: ContainerSummary, deep: boolean) {
  if (!store.selectedHostId.value) return;

  try {
    if (deep) {
      const result = await api.deepRemoveContainer(store.selectedHostId.value, row.id);
      if (result.warnings.length > 0) {
        message.warning(result.warnings.join(" | "));
      } else {
        message.success(t("messages.containerDeepRemoved", { name: row.names }));
      }
    } else {
      await api.removeContainer(store.selectedHostId.value, row.id);
      message.success(t("messages.containerRemoved", { name: row.names }));
    }

    await store.refreshContainers();
    markSynced();
  } catch (error) {
    message.error(getErrorMessage(error));
  }
}

function appendLine(current: string, nextLine: string) {
  return current.length > 0 ? `${current}\n${nextLine}` : nextLine;
}

function isContainerRunning(container: ContainerSummary) {
  return normalizeState(container.state) === "running";
}

function openExec(row: ContainerSummary) {
  if (!store.selectedHostId.value) return;

  if (!isContainerRunning(row)) {
    message.warning(
      locale.value === "zh-CN"
        ? `请先启动容器 ${row.names}，再打开命令会话`
        : `Start container ${row.names} before opening a shell session`,
    );
    return;
  }

  closeExec();
  activeExecContainer.value = row;
  execInput.value = "";
  execOutput.value = t("container.execConnected");
  execVisible.value = true;
  execSocket = new WebSocket(api.execSocketUrl(store.selectedHostId.value, row.id));
  execSocket.onmessage = (event) => {
    execOutput.value = appendLine(execOutput.value, String(event.data));
  };
  execSocket.onerror = () => {
    execOutput.value = appendLine(execOutput.value, t("messages.execSocketFailed"));
  };
  execSocket.onclose = () => {
    execOutput.value = appendLine(execOutput.value, t("container.execDisconnected"));
  };
}

function sendExec() {
  if (execInput.value.trim().length === 0) {
    message.warning(t("messages.commandRequired"));
    return;
  }

  if (!execSocket || execSocket.readyState !== WebSocket.OPEN) {
    message.warning(t("messages.socketNotReady"));
    return;
  }

  execSocket.send(execInput.value);
  execInput.value = "";
  message.success(t("messages.commandSent"));
}

function closeExec() {
  if (execSocket) {
    execSocket.onclose = null;
    execSocket.close();
    execSocket = null;
  }
}

function openLogs(row: ContainerSummary) {
  if (!store.selectedHostId.value) return;

  closeLogs();
  activeLogsContainer.value = row;
  logsOutput.value = t("container.logsConnected");
  logsVisible.value = true;
  logsSocket = new WebSocket(api.logsSocketUrl(store.selectedHostId.value, row.id));
  logsSocket.onmessage = (event) => {
    logsOutput.value = appendLine(logsOutput.value, String(event.data));
  };
  logsSocket.onerror = () => {
    logsOutput.value = appendLine(logsOutput.value, t("messages.logsSocketFailed"));
  };
  logsSocket.onclose = () => {
    logsOutput.value = appendLine(logsOutput.value, t("container.logsDisconnected"));
  };
}

function closeLogs() {
  if (logsSocket) {
    logsSocket.onclose = null;
    logsSocket.close();
    logsSocket = null;
  }
}
</script>

<style scoped>
.workspace {
  position: relative;
  min-height: 100vh;
  padding: 16px;
  overflow: hidden;
}

.backdrop {
  position: fixed;
  border-radius: 999px;
  filter: blur(8px);
  opacity: 0.45;
  pointer-events: none;
}

.backdrop-one {
  top: -160px;
  right: -60px;
  width: 340px;
  height: 340px;
  background: radial-gradient(circle, rgba(28, 100, 242, 0.22) 0%, rgba(28, 100, 242, 0) 70%);
}

.backdrop-two {
  bottom: -180px;
  left: -100px;
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(15, 118, 110, 0.18) 0%, rgba(15, 118, 110, 0) 72%);
}

.panel-surface,
.panel-card {
  position: relative;
  z-index: 1;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  margin-bottom: 14px;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1d4ed8 0%, #0f766e 100%);
  color: #fff;
  font-size: 0.96rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  box-shadow: 0 12px 28px rgba(29, 78, 216, 0.2);
}

.brand-copy h1,
.section-heading h2,
.section-heading h3,
.terminal-modal-header h3 {
  margin: 0;
  font-family: "Segoe UI", "Microsoft YaHei UI", "PingFang SC", sans-serif;
}

.brand-copy h1 {
  font-size: clamp(1.15rem, 1.6vw, 1.8rem);
  line-height: 1.1;
}

.brand-copy p,
.section-heading p,
.terminal-modal-header p,
.toolbar-card p {
  margin: 4px 0 0;
  color: var(--text-muted);
  line-height: 1.45;
}

.brand-kicker,
.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  color: var(--text-subtle);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.brand-badge,
.sync-pill,
.results-pill,
.form-helper,
.host-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  font-size: 0.8rem;
}

.brand-badge {
  padding: 4px 8px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  letter-spacing: normal;
  text-transform: none;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sync-pill {
  padding: 8px 12px;
  background: rgba(248, 250, 252, 0.85);
  color: var(--text-subtle);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.locale-select-wrap {
  width: 132px;
}

:deep(.locale-select-wrap .n-base-selection) {
  border-radius: 12px;
}

.content-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.sidebar-column,
.main-column {
  display: grid;
  gap: 16px;
}

.sidebar-intro,
.hero-panel,
.table-panel {
  padding: 18px;
}

.metric-grid,
.selected-host-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card,
.host-fact-card {
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.88) 100%);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.metric-card span,
.host-fact-card span {
  display: block;
  color: var(--text-subtle);
  font-size: 0.82rem;
}

.metric-card strong,
.host-fact-card strong {
  display: block;
  margin-top: 8px;
  color: var(--text-strong);
  font-size: 1.45rem;
  line-height: 1;
}

.host-fact-card small {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 0.82rem;
}

.accent-blue {
  box-shadow: inset 0 0 0 1px rgba(96, 165, 250, 0.12);
}

.accent-emerald {
  box-shadow: inset 0 0 0 1px rgba(16, 185, 129, 0.12);
}

.accent-amber {
  box-shadow: inset 0 0 0 1px rgba(245, 158, 11, 0.12);
}

.accent-rose {
  box-shadow: inset 0 0 0 1px rgba(244, 63, 94, 0.12);
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-heading.compact h2 {
  font-size: 0.98rem;
  font-weight: 600;
  line-height: 1.5;
}

.section-heading.split {
  align-items: center;
}

.section-heading h2 {
  font-size: 1.08rem;
}

.panel-card :deep(.n-card__content) {
  padding: 18px;
}

.form-row.two-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-helper {
  margin: 4px 0 14px;
  padding: 8px 12px;
  background: rgba(241, 245, 249, 0.9);
  color: var(--text-muted);
}

.action-row {
  width: 100%;
}

.host-list {
  display: grid;
  gap: 10px;
}

.host-tile {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 12px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.9) 100%);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.host-tile.active {
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.1);
  transform: translateY(-1px);
}

.host-tile-main {
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.host-tile-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.host-tile-top strong {
  font-size: 0.96rem;
  color: var(--text-strong);
}

.host-tile p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.host-tile-meta {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  color: var(--text-subtle);
  font-size: 0.8rem;
}

.host-empty-state,
.empty-panel {
  padding: 20px 0;
}

.hero-panel {
  overflow: hidden;
}

.hero-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top right, rgba(29, 78, 216, 0.08), transparent 26%),
    radial-gradient(circle at bottom left, rgba(15, 118, 110, 0.1), transparent 28%);
  pointer-events: none;
}

.priority-panel {
  min-height: calc(100vh - 140px);
}

.panel-heading-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.host-chip {
  padding: 8px 12px;
  background: rgba(29, 78, 216, 0.08);
  border: 1px solid rgba(29, 78, 216, 0.12);
  color: #1d4ed8;
  font-weight: 700;
  max-width: 220px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-heading,
.compact-heading {
  margin-bottom: 12px;
}

.results-pill {
  padding: 8px 12px;
  background: rgba(15, 23, 42, 0.05);
  color: var(--text-subtle);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.toolbar-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  margin-bottom: 14px;
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card h3 {
  margin: 0;
  font-size: 0.96rem;
}

.toolbar-controls {
  display: grid;
  grid-template-columns: minmax(240px, 320px) 180px auto;
  gap: 10px;
  align-items: center;
}

.data-table-shell {
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.94);
  background: rgba(255, 255, 255, 0.92);
}

.data-table-shell-large {
  min-height: 540px;
}

.compact-hero {
  padding-top: 16px;
}

.compact-host-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.compact-card {
  padding: 12px 14px;
}

.compact-card strong {
  font-size: 1.2rem;
}

.terminal-modal {
  width: min(920px, calc(100vw - 32px));
  margin: 24px auto;
  padding: 24px;
  border-radius: 28px;
  background: rgba(10, 16, 28, 0.96);
  color: #f8fafc;
  box-shadow: 0 26px 60px rgba(2, 6, 23, 0.46);
}

.terminal-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.terminal-modal-header p {
  color: rgba(226, 232, 240, 0.72);
}

.terminal-box {
  min-height: 280px;
  max-height: 56vh;
  margin: 18px 0 0;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.96) 0%, rgba(2, 6, 23, 0.98) 100%);
  border: 1px solid rgba(59, 130, 246, 0.15);
  color: #dbeafe;
  font-family: "Cascadia Code", "Consolas", monospace;
  font-size: 0.9rem;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow: auto;
}

.terminal-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  margin-top: 16px;
}

:deep(.n-input),
:deep(.n-base-selection),
:deep(.n-input-number) {
  border-radius: 14px;
}

:deep(.n-input .n-input__border),
:deep(.n-base-selection .n-base-selection__border),
:deep(.n-input-number .n-input-wrapper) {
  border-color: rgba(203, 213, 225, 0.92) !important;
}

:deep(.n-data-table-th) {
  background: rgba(248, 250, 252, 0.95) !important;
  color: var(--text-subtle);
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

:deep(.n-data-table-td) {
  background: rgba(255, 255, 255, 0.9);
}

:deep(.table-id) {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #1d4ed8;
  font-size: 0.8rem;
}

:deep(.table-stack) {
  display: grid;
  gap: 4px;
}

:deep(.table-primary) {
  color: var(--text-strong);
}

:deep(.table-secondary),
:deep(.table-image) {
  color: var(--text-muted);
  font-size: 0.84rem;
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-card {
    grid-template-columns: 1fr;
  }

  .toolbar-controls {
    grid-template-columns: 1fr;
  }

  .compact-host-grid {
    grid-template-columns: 1fr;
  }

  .priority-panel {
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .workspace {
    padding: 12px;
  }

  .topbar,
  .sidebar-intro,
  .hero-panel,
  .table-panel,
  .panel-card :deep(.n-card__content) {
    padding: 14px;
  }

  .topbar,
  .brand-block,
  .topbar-actions,
  .section-heading.split,
  .terminal-modal-header {
    display: grid;
  }

  .selected-host-grid,
  .metric-grid,
  .form-row.two-columns,
  .terminal-input-row,
  .panel-heading-right {
    grid-template-columns: 1fr;
  }

  .locale-select-wrap,
  .host-chip {
    width: 100%;
    max-width: none;
  }
}
</style>
