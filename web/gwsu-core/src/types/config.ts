/**
 * 配置相关类型定义
 */

/** 值类型枚举 */
export enum ConfigValueType {
  /** 字符串 */
  STR = 1,
  /** 数字 */
  NUMBER = 2,
  /** 布尔 */
  BOOL = 3,
  /** JSON */
  JSON = 4,
}

/** 配置类型枚举 */
export enum ConfigType {
  /** 系统配置 */
  SYSTEM = 1,
  /** 自定义配置 */
  CUSTOM = 2,
}

/** 配置信息 */
export interface ConfigVO {
  /** 主键ID */
  id: string;
  /** 配置键 */
  configKey: string;
  /** 配置名称 */
  configName: string;
  /** 配置值 */
  configValue: string;
  /** 值类型：1-STR 2-NUMBER 3-BOOL 4-JSON */
  valueType: ConfigValueType;
  /** 配置类型：1-系统 2-自定义 */
  configType: ConfigType;
  /** 描述 */
  description: string;
}

/** 配置批量查询结果：配置键 -> 配置信息 */
export type ConfigMap = Record<string, ConfigVO>;
