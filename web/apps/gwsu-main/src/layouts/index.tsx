import ThemeSwitcher from '@/components/ThemeSwitcher';
import { CopilotChatPanel } from '@/components/AIChat/CopilotChatPanel';
import AssistantOperationArea from '@/components/AssistantOperationArea';
import {
  PanelProvider,
  usePanelContext,
} from '@/components/AIChat/AIChatContext';
import { GwsuCopilotKitProvider } from '@/providers/CopilotKitProvider';
import {
  ArrowDownOutlined,
  LogoutOutlined,
  RobotOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { App, Button } from 'antd';
import {
  EventType,
  onEvent,
  ThemeLayout,
  useThemeContext,
  useUserStore,
  useMenuStore,
} from '@gwsu/core';
import { useCallback, useEffect, useRef, useState } from 'react';
import { history, Outlet, useLocation } from 'umi';
import { logout } from '@/services/auth';
import styles from './index.module.less';

export default function LayoutComponent() {
  return (
    <ThemeLayout>
      <LayoutRouter />
    </ThemeLayout>
  );
}

/** 路由层：根据是否登录页决定是否初始化 CopilotKit */
function LayoutRouter() {
  const location = useLocation();
  const { currentTheme } = useThemeContext();
  // 访问根路径时自动跳转首页 + 登录事件监听
  useEffect(() => {
    const homePath = process.env.UMI_APP_HOME_PATH as string;
    const loginPath = process.env.UMI_APP_LOGIN_PATH as string;
    if (location.pathname === '/') {
      history.replace(homePath);
    }

    const successEvent = onEvent(EventType.LOGIN_SUCCESS, () => {
      console.log('登录成功');
      history.push(homePath);
    });

    const expireEvent = onEvent(EventType.TOKEN_EXPIRED, () => {
      console.log('登录失效');
      history.push(loginPath);
    });

    return () => {
      successEvent();
      expireEvent();
    };
  }, [location.pathname]);

  // 判断是否是登录页面
  const isLoginPage = location.pathname.includes('/login');

  // 登录页面：不初始化 CopilotKit，使用简单布局
  if (isLoginPage) {
    return (
      <div className={`${styles.mainLayout} ${styles.loginMode}`}>
        <div className={styles.loginContent}>
          <Outlet />
        </div>
      </div>
    );
  }

  // 非登录页面：初始化 CopilotKit
  return (
    <GwsuCopilotKitProvider>
      <PanelProvider>
        <MainLayoutContent currentTheme={currentTheme} />
      </PanelProvider>
    </GwsuCopilotKitProvider>
  );
}

/** 主布局内容（在 CopilotKit 和 PanelProvider 上下文内） */
function MainLayoutContent({
  currentTheme,
}: {
  currentTheme: ReturnType<typeof useThemeContext>['currentTheme'];
}) {
  const { panelState, setPanelMode, togglePanel } = usePanelContext();
  // 悬浮提示相关状态
  const [showGuide, setShowGuide] = useState(false);
  const guideTimerRef = useRef<NodeJS.Timeout | null>(null);
  const robotBtnRef = useRef<HTMLDivElement>(null);
  const { message, modal } = App.useApp();
  // 当面板收起时，显示引导提示
  useEffect(() => {
    if (panelState.mode === 'hidden') {
      guideTimerRef.current = setTimeout(() => {
        setShowGuide(true);
      }, 300);
    } else {
      setShowGuide(false);
    }

    return () => {
      if (guideTimerRef.current) {
        clearTimeout(guideTimerRef.current);
      }
    };
  }, [panelState.mode]);

  // 面板操作
  const handleFixed = useCallback(() => setPanelMode('fixed'), [setPanelMode]);
  const handleDraggable = useCallback(
    () => setPanelMode('draggable'),
    [setPanelMode],
  );
  const handleHide = useCallback(() => setPanelMode('hidden'), [setPanelMode]);
  const handleRobotClick = useCallback(() => {
    setShowGuide(false);
    togglePanel();
  }, [togglePanel]);

  // 处理退出登录
  const handleLogout = () => {
    modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await logout();
          // 通过 userStore 清除认证数据
          useUserStore.getState().logout();
          useMenuStore.getState().clearMenus();

          message.success('退出成功');

          const loginPath =
            process.env.UMI_APP_LOGIN_PATH || '/sub-system/login';
          history.push(loginPath);
        } catch (error) {
          // 错误提示已在 request.ts 中统一处理
        }
      },
    });
  };

  // 判断显示模式
  const isHidden = panelState.mode === 'hidden';
  const isDraggableMode = panelState.mode === 'draggable';

  return (
    <div className={styles.mainLayout}>
      {/* 顶部导航栏 - 固定不变 */}
      <header
        className={styles.mainHeader}
        style={{ background: currentTheme.colors.surface }}
      >
        <div className={styles.headerLeft}>
          <div className={styles.logo}>
            <img
              src="https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg"
              alt="logo"
            />
            <span style={{ color: currentTheme.colors.text }}>GWSU</span>
          </div>
        </div>
        <div className={styles.headerRight}>
          {/* AI 助手图标 - 隐藏状态下显示 */}
          {isHidden && (
            <div ref={robotBtnRef} className={styles.robotBtnWrapper}>
              <Button
                type="text"
                className={styles.headerActionBtn}
                onClick={handleRobotClick}
                icon={<RobotOutlined />}
              />
              {/* 悬浮引导提示 */}
              {showGuide && (
                <div className={styles.guideBubble}>
                  <div className={styles.guideContent}>
                    <span>智能助手</span>
                    <ArrowDownOutlined className={styles.guideArrow} />
                  </div>
                </div>
              )}
            </div>
          )}
          <ThemeSwitcher />
          <span className={`${styles.actionItem} ${styles.userInfo}`}>
            <UserOutlined />
            管理员
          </span>
          <a
            onClick={handleLogout}
            className={`${styles.actionItem} ${styles.logoutBtn}`}
          >
            <LogoutOutlined />
            退出登录
          </a>
        </div>
      </header>

      {/* 下方内容区域 */}
      <div className={styles.contentLayout}>
        {/* AI 聊天区占位 - 固定模式下保留空间 */}
        {!isHidden && !isDraggableMode && (
          <div className={styles.aiChatFixedPlaceholder} />
        )}

        {/* 智能助手操作区 - 能力容器 */}
        <AssistantOperationArea/>
      </div>

      {/* AI 聊天面板 - 始终渲染，通过 mode 属性控制显示模式，避免重新初始化 */}
      <CopilotChatPanel
        fixedWidth="100%"
        mode={panelState.mode}
        onFixed={handleFixed}
        onDraggable={handleDraggable}
        onHide={handleHide}
      />
    </div>
  );
}
