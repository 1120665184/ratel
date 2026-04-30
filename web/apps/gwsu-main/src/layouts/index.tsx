import ThemeSwitcher from '@/components/ThemeSwitcher';
import { CopilotChatPanel } from '@/components/AIChat/CopilotChatPanel';
import AssistantOperationArea from '@/components/AssistantOperationArea';
import { PanelProvider, usePanelContext } from '@/components/AIChat/AIChatContext';
import { GwsuCopilotKitProvider } from '@/providers/CopilotKitProvider';
import { LogoutOutlined, RobotOutlined, UserOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { Button, message, Modal } from 'antd';
import { EventType, onEvent, ThemeLayout, useThemeContext } from '@gwsu/core';
import { useCallback, useEffect, useState, useRef } from 'react';
import { history, MicroApp, useLocation } from 'umi';
import { logout } from '@/services/auth';
import styles from './index.module.less';

export default function LayoutComponent() {
  return (
    <ThemeLayout>
      <GwsuCopilotKitProvider>
        <PanelProvider>
          <LayoutComponentInner />
        </PanelProvider>
      </GwsuCopilotKitProvider>
    </ThemeLayout>
  );
}

function LayoutComponentInner() {
  const location = useLocation();
  const { currentTheme } = useThemeContext();
  const { panelState, setPanelMode, togglePanel } = usePanelContext();

  // 悬浮提示相关状态
  const [showGuide, setShowGuide] = useState(false);
  const guideTimerRef = useRef<NodeJS.Timeout | null>(null);
  const robotBtnRef = useRef<HTMLDivElement>(null);

  // 切换到固定模式
  const handleFixed = useCallback(() => {
    setPanelMode('fixed');
  }, [setPanelMode]);

  // 切换到拖拽模式
  const handleDraggable = useCallback(() => {
    setPanelMode('draggable');
  }, [setPanelMode]);

  // 隐藏面板
  const handleHide = useCallback(() => {
    setPanelMode('hidden');
  }, [setPanelMode]);

  // 点击机器人图标
  const handleRobotClick = useCallback(() => {
    setShowGuide(false);
    togglePanel();
  }, [togglePanel]);

  // 当面板收起时，显示引导提示
  useEffect(() => {
    if (panelState.mode === 'hidden') {
      // 延迟显示提示，让用户先看到收起动画
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

  // 访问根路径时自动跳转首页
  useEffect(() => {
    const homePath = process.env.UMI_APP_HOME_PATH as string;
    const loginPath = process.env.UMI_APP_LOGIN_PATH as string;
    if (location.pathname === '/') {
      history.replace(homePath);
    }

    // 登录成功事件监听
    let successEvent = onEvent(EventType.LOGIN_SUCCESS, () => {
      console.log('登录成功');
      history.push(homePath);
    });

    // 登录失败事件监听
    let expireEvent = onEvent(EventType.TOKEN_EXPIRED, () => {
      console.log('登录失效');
      history.push(loginPath);
    });

    return () => {
      successEvent();
      expireEvent();
    };
  }, [location.pathname]);

  // 处理退出登录
  const handleLogout = () => {
    Modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await logout();
          // 清除本地存储的认证信息
          localStorage.removeItem('token');
          localStorage.removeItem('userId');
          localStorage.removeItem('isLoggedIn');
          localStorage.removeItem('tokenExpireTime');

          message.success('退出成功');

          // 跳转到登录页
          const loginPath = process.env.UMI_APP_LOGIN_PATH || '/sub-system/login';
          history.push(loginPath);
        } catch (error) {
          // 错误提示已在 request.ts 中统一处理
        }
      },
    });
  };

  // 判断当前应用
  const currentApp = location.pathname.startsWith('/sub-security')
    ? 'gwsu-sub-security'
    : 'gwsu-sub-system';

  // 判断是否是登录页面
  const isLoginPage = location.pathname.includes('/login');

  // 如果是登录页面，使用简单的布局
  if (isLoginPage) {
    return (
      <div className={`${styles.mainLayout} ${styles.loginMode}`}>
        <div className={styles.loginContent}>
          <MicroApp name={currentApp} />
        </div>
      </div>
    );
  }

  // 判断显示模式
  const isHidden = panelState.mode === 'hidden';
  const isDraggableMode = panelState.mode === 'draggable';

  return (
    <div className={styles.mainLayout}>
      {/* 顶部导航栏 - 固定不变 */}
      <header className={styles.mainHeader} style={{ background: currentTheme.colors.surface }}>
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
          <a onClick={handleLogout} className={`${styles.actionItem} ${styles.logoutBtn}`}>
            <LogoutOutlined />
            退出登录
          </a>
        </div>
      </header>

      {/* 下方内容区域 */}
      <div className={styles.contentLayout}>
        {/* AI 聊天区占位 - 固定模式下保留空间 */}
        {!isHidden && !isDraggableMode && <div className={styles.aiChatFixedPlaceholder} />}

        {/* 智能助手操作区 - 能力容器 */}
        <AssistantOperationArea />
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
