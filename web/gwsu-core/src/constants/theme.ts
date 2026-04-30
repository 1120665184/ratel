import type { ThemeConfig } from '../types';

// 深海蓝主题 - 专业稳重
export const oceanTheme: ThemeConfig = {
  name: '深海蓝',
  key: 'ocean',
  colors: {
    primary: '#1a5fb4',
    primaryLight: '#3584e4',
    primaryDark: '#0d3a7a',
    background: '#f6f8fc',
    surface: '#ffffff',
    text: '#1a1a2e',
    textSecondary: '#5a5a6e',
    border: '#e1e5eb',
    success: '#26a269',
    warning: '#e5a50a',
    error: '#c01c28',
    info: '#1c71d8',
  },
};

// 森林绿主题 - 自然清新
export const forestTheme: ThemeConfig = {
  name: '森林绿',
  key: 'forest',
  colors: {
    primary: '#2d6a4f',
    primaryLight: '#40916c',
    primaryDark: '#1b4332',
    background: '#f5f9f7',
    surface: '#ffffff',
    text: '#1a1a2e',
    textSecondary: '#5a5a6e',
    border: '#d8e8e0',
    success: '#2d6a4f',
    warning: '#d4a373',
    error: '#bc4749',
    info: '#52796f',
  },
};

// 紫罗兰主题 - 优雅现代
export const violetTheme: ThemeConfig = {
  name: '紫罗兰',
  key: 'violet',
  colors: {
    primary: '#6b4c9a',
    primaryLight: '#9b7ed9',
    primaryDark: '#4a3570',
    background: '#faf8fc',
    surface: '#ffffff',
    text: '#1a1a2e',
    textSecondary: '#6a6a7e',
    border: '#e8e0f0',
    success: '#5a9a6b',
    warning: '#d4a35a',
    error: '#c94c4c',
    info: '#7e9ad9',
  },
};

// 琥珀橙主题 - 活力温暖
export const amberTheme: ThemeConfig = {
  name: '琥珀橙',
  key: 'amber',
  colors: {
    primary: '#c2510c',
    primaryLight: '#e67e22',
    primaryDark: '#9c4010',
    background: '#fdf8f3',
    surface: '#ffffff',
    text: '#1a1a2e',
    textSecondary: '#6a6a6e',
    border: '#f0e0d0',
    success: '#27ae60',
    warning: '#f39c12',
    error: '#e74c3c',
    info: '#3498db',
  },
};

// 石墨灰主题 - 简约商务
export const graphiteTheme: ThemeConfig = {
  name: '石墨灰',
  key: 'graphite',
  colors: {
    primary: '#374151',
    primaryLight: '#4b5563',
    primaryDark: '#1f2937',
    background: '#f3f4f6',
    surface: '#ffffff',
    text: '#111827',
    textSecondary: '#6b7280',
    border: '#e5e7eb',
    success: '#059669',
    warning: '#d97706',
    error: '#dc2626',
    info: '#2563eb',
  },
};

// 午夜暗色主题 - 深邃专业
export const midnightTheme: ThemeConfig = {
  name: '午夜暗色',
  key: 'midnight',
  colors: {
    primary: '#60a5fa',
    primaryLight: '#93c5fd',
    primaryDark: '#3b82f6',
    background: '#0f172a',
    surface: '#1e293b',
    text: '#f1f5f9',
    textSecondary: '#94a3b8',
    border: '#334155',
    success: '#4ade80',
    warning: '#fbbf24',
    error: '#f87171',
    info: '#60a5fa',
  },
};

export const themes: ThemeConfig[] = [
  oceanTheme,
  forestTheme,
  violetTheme,
  amberTheme,
  graphiteTheme,
  midnightTheme,
];

export const defaultTheme = oceanTheme;

export const getThemeByKey = (key: string): ThemeConfig => {
  return themes.find((t) => t.key === key) || defaultTheme;
};

// 主题映射表（用于子应用快速查找）
export const themeMap: Record<string, ThemeConfig> = {
  ocean: oceanTheme,
  forest: forestTheme,
  violet: violetTheme,
  amber: amberTheme,
  graphite: graphiteTheme,
  midnight: midnightTheme,
};
