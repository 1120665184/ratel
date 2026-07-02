/** 基础地址配置 */
export interface BaseUrlConfig {
  /** 项目名称 */
  projectName: string;
  /** 前端地址 */
  viewBaseUrl: string;
  /** 后端 API 地址 */
  apiBaseUrl: string;
}

/** 基础地址默认配置 */
export const DEFAULT_BASE_URL_CONFIG: BaseUrlConfig = {
  projectName: 'Ratel',
  viewBaseUrl: 'http://127.0.0.1:8000',
  apiBaseUrl: 'http://127.0.0.1:8888',
};

/** 创建默认的基础地址配置 */
export function createDefaultBaseUrlConfig(): BaseUrlConfig {
  return { ...DEFAULT_BASE_URL_CONFIG };
}

/** 基础地址配置 key */
export const BASE_URL_CONFIG_KEY = 'basic_url_config';

/** 通用配置 Tab 标识 */
export type GeneralTabKey = 'projectUrl';
