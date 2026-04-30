import type { ThemeConfig } from '../types';
import { EventType, emitEvent } from '../constants/events';

const THEME_STORAGE_KEY = 'gwsu-theme';

/**
 * 应用主题到 CSS 变量
 */
export const applyTheme = (theme: ThemeConfig): void => {
  const root = document.documentElement;
  const colors = theme.colors;
  const isDarkTheme = theme.key === 'midnight';

  // 设置 CSS 变量
  root.style.setProperty('--primary-color', colors.primary);
  root.style.setProperty('--primary-color-light', colors.primaryLight);
  root.style.setProperty('--primary-color-dark', colors.primaryDark);
  root.style.setProperty('--background-color', colors.background);
  root.style.setProperty('--surface-color', colors.surface);
  root.style.setProperty('--text-color', colors.text);
  root.style.setProperty('--text-secondary-color', colors.textSecondary);
  root.style.setProperty('--border-color', colors.border);
  root.style.setProperty('--success-color', colors.success);
  root.style.setProperty('--warning-color', colors.warning);
  root.style.setProperty('--error-color', colors.error);
  root.style.setProperty('--info-color', colors.info);

  // 设置布局相关变量
  root.style.setProperty('--header-bg', isDarkTheme ? '#1f2937' : colors.surface);
  root.style.setProperty('--sider-bg', isDarkTheme ? '#1f2937' : colors.surface);
  root.style.setProperty('--menu-text', isDarkTheme ? '#94a3b8' : colors.textSecondary);
  root.style.setProperty('--menu-text-active', isDarkTheme ? '#60a5fa' : colors.primary);
  root.style.setProperty('--menu-bg-active', isDarkTheme ? 'rgba(96, 165, 250, 0.15)' : `${colors.primary}20`);

  // 设置 body 背景色
  document.body.style.backgroundColor = colors.background;

  // 设置 data-theme 属性用于 CSS 选择器
  root.setAttribute('data-theme', theme.key);

  // 更新 layout wrapper 的 data-theme
  const layoutWrapper = document.querySelector('.theme-layout-wrapper') as HTMLElement;
  if (layoutWrapper) {
    layoutWrapper.setAttribute('data-theme', theme.key);
  }
};

/**
 * 保存主题到 localStorage
 */
export const saveTheme = (themeKey: string): void => {
  localStorage.setItem(THEME_STORAGE_KEY, themeKey);
};

/**
 * 从 localStorage 获取主题 key
 */
export const getSavedThemeKey = (): string | null => {
  return localStorage.getItem(THEME_STORAGE_KEY);
};

/**
 * 通知子应用主题变更
 */
export const notifyThemeChange = (theme: ThemeConfig): void => {
  emitEvent(EventType.THEME_CHANGE, theme);
};

/**
 * 获取 Ant Design 主题配置
 */
export const getAntdThemeConfig = (theme: ThemeConfig) => {
  const isDarkTheme = theme.key === 'midnight';
  
  return {
    token: {
      colorPrimary: theme.colors.primary,
      colorSuccess: theme.colors.success,
      colorWarning: theme.colors.warning,
      colorError: theme.colors.error,
      colorInfo: theme.colors.info,
      colorBgBase: theme.colors.background,
      colorTextBase: theme.colors.text,
      colorBorder: theme.colors.border,
    },
    algorithm: isDarkTheme ? 'dark' : 'default',
  };
};
