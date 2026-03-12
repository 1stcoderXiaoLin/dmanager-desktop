import { describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { useDockerStore } from "./useDockerStore";

const apiMocks = vi.hoisted(() => ({
  listHosts: vi.fn(async () => [
    {
      id: "h1",
      name: "dev",
      address: "127.0.0.1",
      port: 22,
      username: "root",
      authType: "PASSWORD",
    },
  ]),
  createHost: vi.fn(async () => ({
    id: "h2",
    name: "prod",
    address: "10.0.0.10",
    port: 22,
    username: "root",
    authType: "PASSWORD",
  })),
  deleteHost: vi.fn(async () => undefined),
  listContainers: vi.fn(async () => []),
}));

vi.mock("../api/client", () => ({
  api: apiMocks,
}));

describe("useDockerStore", () => {
  it("reloadHosts should set selected host automatically", async () => {
    const store = useDockerStore();

    await store.reloadHosts();
    await nextTick();

    expect(store.hosts.value.length).toBe(1);
    expect(store.selectedHostId.value).toBe("h1");
  });

  it("refreshContainers should clear data when no host is selected", async () => {
    const store = useDockerStore();
    store.containers.value = [
      {
        id: "c1",
        names: "demo",
        image: "nginx:latest",
        state: "running",
        status: "Up 5 minutes",
      },
    ];

    await store.refreshContainers();

    expect(store.containers.value).toEqual([]);
    expect(apiMocks.listContainers).not.toHaveBeenCalled();
  });
});
