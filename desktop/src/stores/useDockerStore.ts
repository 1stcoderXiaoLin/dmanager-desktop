import { computed, ref } from "vue";
import { api } from "../api/client";
import type { ContainerSummary, CreateHostPayload, RemoteHost } from "../types";

export function useDockerStore() {
  const hosts = ref<RemoteHost[]>([]);
  const selectedHostId = ref<string | null>(null);
  const containers = ref<ContainerSummary[]>([]);
  const loading = ref(false);

  const selectedHost = computed(
    () => hosts.value.find((host) => host.id === selectedHostId.value) ?? null,
  );

  async function reloadHosts() {
    hosts.value = await api.listHosts();

    if (!hosts.value.some((host) => host.id === selectedHostId.value)) {
      selectedHostId.value = hosts.value[0]?.id ?? null;
    }
  }

  async function addHost(payload: CreateHostPayload) {
    const host = await api.createHost(payload);
    await reloadHosts();
    selectedHostId.value = host.id;
    return host;
  }

  async function removeHost(hostId: string) {
    await api.deleteHost(hostId);
    hosts.value = hosts.value.filter((host) => host.id !== hostId);

    if (selectedHostId.value === hostId) {
      selectedHostId.value = hosts.value[0]?.id ?? null;
    }

    if (!selectedHostId.value) {
      containers.value = [];
    }
  }

  async function refreshContainers() {
    if (!selectedHostId.value) {
      containers.value = [];
      return;
    }

    loading.value = true;
    try {
      containers.value = await api.listContainers(selectedHostId.value);
    } finally {
      loading.value = false;
    }
  }

  return {
    hosts,
    selectedHostId,
    selectedHost,
    containers,
    loading,
    reloadHosts,
    addHost,
    removeHost,
    refreshContainers,
  };
}
