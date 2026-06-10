import { post } from '../utils/request';

export interface Workspace {
    id: string;
    name: string;
}

interface WorkspaceListRaw {
    currWorkspace: Workspace;
    list: Workspace[];
}

export interface WorkspaceListData {
    workspaces: Workspace[];
    currentWorkspaceId: string;
}

export async function fetchWorkspaceList(): Promise<WorkspaceListData> {
    const response = await post<WorkspaceListRaw>('/system/workspace/list');
    const raw = response.data;
    return {
        workspaces: raw?.list ?? [],
        currentWorkspaceId: raw?.currWorkspace?.id ?? '',
    };
}

export async function switchWorkspace(workspaceId: string): Promise<void> {
    await post(`/system/workspace/switch/${workspaceId}`);
}
