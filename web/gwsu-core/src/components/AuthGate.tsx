/**
 * 权限门卫组件
 * 根据按钮权限控制内容是否渲染，不限于按钮，任何需要权限控制的内容均可使用
 */

import React from 'react';
import { useAuth } from '../hooks/useAuth';

interface AuthGateProps {
  /** 按钮标识 */
  buttonKey: string;
  /** 有权限时渲染的内容 */
  children: React.ReactNode;
  /** 无权限时的替代内容，默认不渲染 */
  fallback?: React.ReactNode;
}

const AuthGate: React.FC<AuthGateProps> = ({ buttonKey, children, fallback = null }) => {
  const hasAuth = useAuth(buttonKey);
  return hasAuth ? <>{children}</> : <>{fallback}</>;
};

export default AuthGate;
