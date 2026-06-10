import { ApartmentOutlined, CheckOutlined, LoadingOutlined } from '@ant-design/icons';
import { useWorkspaceStore } from '@gwsu/core';
import type { MenuProps } from 'antd';
import { Dropdown, Tooltip } from 'antd';
import { useEffect } from 'react';
import styles from './index.module.less';

export default function WorkspaceSwitcher() {
    const workspaces = useWorkspaceStore((s) => s.workspaces);
    const currentWorkspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);
    const loading = useWorkspaceStore((s) => s.loading);
    const loadWorkspaces = useWorkspaceStore((s) => s.loadWorkspaces);
    const switchTo = useWorkspaceStore((s) => s.switchTo);

    useEffect(() => {
        loadWorkspaces();
    }, [loadWorkspaces]);

    const currentWorkspace = (workspaces ?? []).find(
        (w) => w.id === currentWorkspaceId,
    );

    const items: MenuProps['items'] = (workspaces ?? []).map((workspace) => ({
        key: workspace.id,
        label: (
            <div className={styles.workspaceItem}>
                <span className={styles.workspaceName}>
                    {workspace.name}
                </span>
                {workspace.id === currentWorkspaceId && (
                    <CheckOutlined className={styles.checkIcon} />
                )}
            </div>
        ),
        onClick: () => {
            if (workspace.id !== currentWorkspaceId) {
                switchTo(workspace.id);
            }
        },
    }));

    return (
        <Dropdown
            menu={{ items, selectedKeys: [currentWorkspaceId] }}
            placement="bottomRight"
            classNames={{ root: styles.dropdown }}
            trigger={['click']}
        >
            <Tooltip title="切换工作区">
                <div className={styles.switcher}>
                    {loading ? (
                        <LoadingOutlined className={styles.icon} />
                    ) : (
                        <ApartmentOutlined className={styles.icon} />
                    )}
                    {currentWorkspace && (
                        <span className={styles.workspaceLabel}>
                            {currentWorkspace.name}
                        </span>
                    )}
                </div>
            </Tooltip>
        </Dropdown>
    );
}
