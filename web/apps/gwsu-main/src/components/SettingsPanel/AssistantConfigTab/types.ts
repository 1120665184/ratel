export type { ConfigInfo, ConfigQuery } from '../services/config';

/** 助手配置表单值 */
export interface AssistantConfigFormValues {
  model?: string;
  temperature?: number;
  [key: string]: unknown;
}
