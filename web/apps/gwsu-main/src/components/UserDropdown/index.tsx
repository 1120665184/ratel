import { LogoutOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons';
import { App, Avatar, Dropdown, Tooltip } from 'antd';
import { useUserStore, useMenuStore } from '@gwsu/core';
import { history } from 'umi';
import { logout } from '@/services/auth';
import styles from './index.module.less';

/** 获取用户名首字母（支持中文取第一个字） */
function getInitial(name: string): string {
  if (!name) return 'U';
  return name.charAt(0).toUpperCase();
}

export default function UserDropdown() {
  const userInfo = useUserStore((s) => s.userInfo);
  const { message, modal } = App.useApp();

  const displayName = userInfo?.nickname || userInfo?.username || '用户';
  const email = userInfo?.email;
  const avatarUrl = userInfo?.avatar;
  const initial = getInitial(displayName);

  // 退出登录
  const handleLogout = () => {
    modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await logout();
          useUserStore.getState().logout();
          useMenuStore.getState().clearMenus();
          message.success('退出成功');
          const loginPath = process.env.UMI_APP_LOGIN_PATH || '/sub-system/login';
          history.push(loginPath);
        } catch {
          // 错误提示已在 request.ts 中统一处理
        }
      },
    });
  };

  // 个人信息
  const handleProfile = () => {
    history.push('/sub-system/profile');
  };

  // 下拉面板内容
  const dropdownContent = (
    <div className={styles.dropdownPanel}>
      {/* 信息头 */}
      <div className={styles.infoHeader}>
        <Avatar
          className={styles.infoAvatar}
          size={40}
          src={avatarUrl || undefined}
          style={
            avatarUrl
              ? undefined
              : {
                  background:
                    'linear-gradient(135deg, var(--primary-color, #1a5fb4), #764ba2)',
                  color: '#fff',
                  fontWeight: 600,
                }
          }
        >
          {avatarUrl ? undefined : initial}
        </Avatar>
        <div className={styles.infoDetail}>
          <div className={styles.infoName}>{displayName}</div>
          {email && <div className={styles.infoEmail}>{email}</div>}
        </div>
        <Tooltip title="设置">
          <div className={styles.settingsBtn}>
            <SettingOutlined />
          </div>
        </Tooltip>
      </div>

      {/* 菜单项 */}
      <div className={styles.menuItem} onClick={handleProfile}>
        <UserOutlined className={styles.menuItemIcon} />
        <span>个人信息</span>
      </div>
      <div
        className={`${styles.menuItem} ${styles.dangerItem}`}
        onClick={handleLogout}
      >
        <LogoutOutlined className={styles.menuItemIcon} />
        <span>退出登录</span>
      </div>
    </div>
  );

  return (
    <Dropdown
      dropdownRender={() => dropdownContent}
      placement="bottomRight"
      trigger={['click']}
    >
      <div className={styles.trigger}>
        <Avatar
          className={styles.triggerAvatar}
          size={32}
          src={avatarUrl || undefined}
          style={
            avatarUrl
              ? undefined
              : {
                  background:
                    'linear-gradient(135deg, var(--primary-color, #1a5fb4), #764ba2)',
                  color: '#fff',
                  fontWeight: 600,
                  fontSize: 14,
                }
          }
        >
          {avatarUrl ? undefined : initial}
        </Avatar>
        <span className={styles.triggerName}>{displayName}</span>
      </div>
    </Dropdown>
  );
}
