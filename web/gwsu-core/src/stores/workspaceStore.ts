import { createStore } from 'zustand/vanilla';
import type { StoreApi } from 'zustand/vanilla';
import { useStore } from 'zustand';
import type { Workspace } from '../services/workspace';
import { fetchWorkspaceList, switchWorkspace } from '../services/workspace';

const rawWindow: Window & typeof globalThis & Record<string, unknown> = (0, eval)('window');

const STORE_KEY = '__GWSU_WORKSPACE_STORE__';

interface WorkspaceState {
    workspaces: Workspace[];
    currentWorkspaceId: string;
    loading: boolean;

    setWorkspaces: (workspaces: Workspace[]) => void;
    setCurrentWorkspaceId: (id: string) => void;
    loadWorkspaces: () => Promise<void>;
    switchTo: (workspaceId: string) => Promise<void>;
    getCurrentWorkspace: () => Workspace | undefined;
}

function createOrGetStore(): StoreApi<WorkspaceState> {
    if (rawWindow[STORE_KEY]) {
        return rawWindow[STORE_KEY] as StoreApi<WorkspaceState>;
    }

    const store = createStore<WorkspaceState>((set, get) => ({
        workspaces: [],
        currentWorkspaceId: '',
        loading: false,

        setWorkspaces: (workspaces) => set({ workspaces }),

        setCurrentWorkspaceId: (id) => set({ currentWorkspaceId: id }),

        loadWorkspaces: async () => {
            set({ loading: true });
            try {
                const data = await fetchWorkspaceList();
                set({
                    workspaces: data?.workspaces ?? [],
                    currentWorkspaceId: data?.currentWorkspaceId ?? '',
                    loading: false,
                });
            } catch {
                set({ loading: false });
            }
        },

        switchTo: async (workspaceId) => {
            const { currentWorkspaceId } = get();
            if (workspaceId === currentWorkspaceId) return;
            set({ loading: true });
            try {
                await switchWorkspace(workspaceId);
                set({ currentWorkspaceId: workspaceId, loading: false });
                window.location.reload();
            } catch {
                set({ loading: false });
            }
        },

        getCurrentWorkspace: () => {
            const { workspaces, currentWorkspaceId } = get();
            return workspaces.find((w) => w.id === currentWorkspaceId);
        },
    }));

    rawWindow[STORE_KEY] = store;
    return store;
}

const vanillaStore = createOrGetStore();

function useWorkspaceStore(): WorkspaceState;
function useWorkspaceStore<U>(selector: (state: WorkspaceState) => U): U;
function useWorkspaceStore<U>(selector?: (state: WorkspaceState) => U): WorkspaceState | U {
    return useStore(vanillaStore, selector as (state: WorkspaceState) => U);
}

useWorkspaceStore.getState = vanillaStore.getState;
useWorkspaceStore.setState = vanillaStore.setState;
useWorkspaceStore.subscribe = vanillaStore.subscribe;
useWorkspaceStore.getInitialState = vanillaStore.getInitialState;

export { useWorkspaceStore };
