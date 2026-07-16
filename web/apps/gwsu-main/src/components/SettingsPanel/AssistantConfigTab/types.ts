/** 助手展示配置 */
export interface ViewConfig {
  /** 是否展示思考内容 */
  showThinking: boolean;
  /** 是否展示工具调用 */
  showToolCalls: boolean;
  /** 是否展示历史记录 */
  showHistory: boolean;
  /** 是否启用拖拽模式 */
  enableDragMode: boolean;
}

/** 展示配置默认值 */
export const DEFAULT_VIEW_CONFIG: ViewConfig = {
  showThinking: true,
  showToolCalls: true,
  showHistory: true,
  enableDragMode: false,
};

/** 创建默认的展示配置 */
export function createDefaultViewConfig(): ViewConfig {
  return { ...DEFAULT_VIEW_CONFIG };
}

/** 远程操作类型 */
export type RemoteControlType = 'NONE' | 'DING_TALK';

/** 钉钉远程操作配置 */
export interface DingTalkRemoteConfig {
  /** 协议 */
  protocol: string;
  /** 区域 */
  regionId: string;
  /** 端点 */
  endpoint: string;
  /** Client ID */
  clientId: string;
  /** Client Secret */
  clientSecret: string;
  /** AI 输出卡片模板 ID */
  aiCardTemplateId: string;
}

/** 远程操作配置 */
export interface RemoteControlConfig {
  /** 类型 */
  type: RemoteControlType;
  /** 钉钉配置 */
  dingTalk: DingTalkRemoteConfig;
}

/** 钉钉远程操作默认配置 */
export const DEFAULT_DINGTALK_REMOTE_CONFIG: DingTalkRemoteConfig = {
  protocol: 'https',
  regionId: 'central',
  endpoint: 'api.dingtalk.com',
  clientId: '',
  clientSecret: '',
  aiCardTemplateId: '7f991cfb-9c52-4bac-aad2-5c60116d82cc.schema',
};

/** 创建默认的远程操作配置 */
export function createDefaultRemoteControlConfig(): RemoteControlConfig {
  return {
    type: 'NONE',
    dingTalk: { ...DEFAULT_DINGTALK_REMOTE_CONFIG },
  };
}

/** 远程操作类型选项 */
export const REMOTE_CONTROL_TYPE_OPTIONS = [
  { label: '无', value: 'NONE' as RemoteControlType },
  { label: '钉钉', value: 'DING_TALK' as RemoteControlType },
];

/** 助手配置 Tab 标识 */
export type AssistantTabKey = 'view' | 'remote';
