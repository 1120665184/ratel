export type { ConfigInfo, ConfigQuery } from '../services/config';

/** 自定义配置表单值 */
export interface CustomConfigFormValues {
  configKey: string;
  configName: string;
  configValue: string;
  valueType: number;
  description?: string;
}
