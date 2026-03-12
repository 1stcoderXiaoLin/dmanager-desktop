import type { ContainerSummary, CreateHostPayload, DeleteResponse, RemoteHost } from "../types";

const BASE_URL = "http://127.0.0.1:18080";
const WS_BASE_URL = BASE_URL.replace(/^http/, "ws");

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
    },
    ...init,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `请求失败: ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  listHosts: () => request<RemoteHost[]>("/api/hosts"),
  createHost: (payload: CreateHostPayload) =>
    request<RemoteHost>("/api/hosts", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  deleteHost: (hostId: string) =>
    request<void>(`/api/hosts/${hostId}`, {
      method: "DELETE",
    }),
  testHost: (hostId: string) =>
    request<{ ok: boolean }>(`/api/hosts/${hostId}/test`, {
      method: "POST",
    }),
  listContainers: (hostId: string) =>
    request<ContainerSummary[]>(`/api/hosts/${hostId}/containers`),
  startContainer: (hostId: string, containerId: string) =>
    request(`/api/hosts/${hostId}/containers/${containerId}/start`, { method: "POST" }),
  removeContainer: (hostId: string, containerId: string) =>
    request<DeleteResponse>(`/api/hosts/${hostId}/containers/${containerId}`, { method: "DELETE" }),
  deepRemoveContainer: (hostId: string, containerId: string) =>
    request<DeleteResponse>(`/api/hosts/${hostId}/containers/${containerId}/deep`, {
      method: "DELETE",
    }),
  execSocketUrl: (hostId: string, containerId: string) =>
    `${WS_BASE_URL}/api/ws/hosts/${hostId}/containers/${containerId}/exec`,
  logsSocketUrl: (hostId: string, containerId: string) =>
    `${WS_BASE_URL}/api/ws/hosts/${hostId}/containers/${containerId}/logs`,
};
