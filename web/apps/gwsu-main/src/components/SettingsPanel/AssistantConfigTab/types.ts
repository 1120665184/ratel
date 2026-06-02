/** 模型提供商标识 */
export type ModelProvider = 'dashscope' | 'openai' | 'gemini' | 'anthropic';

/** DashScope 提供商配置 */
export interface DashscopeConfig {
  apiKey: string;
  modelName: string;
  stream: boolean;
  enableThinking: boolean;
  enableSearch: boolean;
  baseUrl: string;
}

/** OpenAI 提供商配置 */
export interface OpenaiConfig {
  apiKey: string;
  modelName: string;
  stream: boolean;
  baseUrl: string;
  endpointPath: string;
}

/** Gemini 提供商配置 */
export interface GeminiConfig {
  apiKey: string;
  modelName: string;
  stream: boolean;
  project: string;
  location: string;
}

/** Anthropic 提供商配置 */
export interface AnthropicConfig {
  apiKey: string;
  modelName: string;
  stream: boolean;
  baseUrl: string;
}

/** 通用生成参数 */
export interface GenerateOptionsConfig {
  temperature?: number;
  topP?: number;
  maxTokens?: number;
  frequencyPenalty?: number;
  presencePenalty?: number;
  topK?: number;
  seed?: number;
  /** 自定义请求体参数，以 JSON 格式配置，用于传递提供商特有的非标准参数 */
  additionalBodyParams?: Record<string, unknown>;
}

/** 助手配置完整结构（与后端 AssistantConfigDTO 一一对应） */
export interface AssistantConfig {
  provider: ModelProvider;
  dashscope: DashscopeConfig;
  openai: OpenaiConfig;
  gemini: GeminiConfig;
  anthropic: AnthropicConfig;
  generateOptions: GenerateOptionsConfig;
}

/** 各提供商的默认配置 */
export const DEFAULT_DASHSCOPE_CONFIG: DashscopeConfig = {
  apiKey: '',
  modelName: 'qwen-plus',
  stream: true,
  enableThinking: false,
  enableSearch: false,
  baseUrl: '',
};

export const DEFAULT_OPENAI_CONFIG: OpenaiConfig = {
  apiKey: '',
  modelName: 'gpt-4.1-mini',
  stream: true,
  baseUrl: '',
  endpointPath: '',
};

export const DEFAULT_GEMINI_CONFIG: GeminiConfig = {
  apiKey: '',
  modelName: 'gemini-2.0-flash',
  stream: true,
  project: '',
  location: 'us-central1',
};

export const DEFAULT_ANTHROPIC_CONFIG: AnthropicConfig = {
  apiKey: '',
  modelName: 'claude-sonnet-4-5-20250929',
  stream: true,
  baseUrl: '',
};

export const DEFAULT_GENERATE_OPTIONS: GenerateOptionsConfig = {
  temperature: 0.2,
  topP: 0.75,
  frequencyPenalty: 0.5,
  presencePenalty: 0.5,
  additionalBodyParams: {},
};

/** 提供商默认配置映射 */
export const PROVIDER_DEFAULTS: Record<ModelProvider, DashscopeConfig | OpenaiConfig | GeminiConfig | AnthropicConfig> = {
  dashscope: DEFAULT_DASHSCOPE_CONFIG,
  openai: DEFAULT_OPENAI_CONFIG,
  gemini: DEFAULT_GEMINI_CONFIG,
  anthropic: DEFAULT_ANTHROPIC_CONFIG,
};

/** 提供商显示信息 */
export interface ProviderInfo {
  key: ModelProvider;
  label: string;
  description: string;
}

export const PROVIDER_LIST: ProviderInfo[] = [
  { key: 'dashscope', label: 'DashScope', description: '阿里云通义千问' },
  { key: 'openai', label: 'OpenAI', description: 'GPT 系列模型' },
  { key: 'gemini', label: 'Gemini', description: 'Google Gemini 模型' },
  { key: 'anthropic', label: 'Anthropic', description: 'Claude 系列模型' },
];

/** DashScope 可选模型 */
export const DASHSCOPE_MODELS = [
  { label: 'Qwen Plus', value: 'qwen-plus' },
  { label: 'Qwen Max', value: 'qwen-max' },
  { label: 'Qwen Turbo', value: 'qwen-turbo' },
  { label: 'QwQ Plus', value: 'qwq-plus' },
];

/** OpenAI 可选模型 */
export const OPENAI_MODELS = [
  { label: 'GPT-4.1 Mini', value: 'gpt-4.1-mini' },
  { label: 'GPT-4.1', value: 'gpt-4.1' },
  { label: 'GPT-4o', value: 'gpt-4o' },
  { label: 'O4 Mini', value: 'o4-mini' },
];

/** Gemini 可选模型 */
export const GEMINI_MODELS = [
  { label: 'Gemini 2.0 Flash', value: 'gemini-2.0-flash' },
  { label: 'Gemini 2.5 Flash', value: 'gemini-2.5-flash' },
  { label: 'Gemini 2.5 Pro', value: 'gemini-2.5-pro' },
];

/** Anthropic 可选模型 */
export const ANTHROPIC_MODELS = [
  { label: 'Claude Sonnet 4.5', value: 'claude-sonnet-4-5-20250929' },
  { label: 'Claude Opus 4', value: 'claude-opus-4' },
  { label: 'Claude Haiku 4.5', value: 'claude-haiku-4-5-20251001' },
];

/** 创建默认的助手配置 */
export function createDefaultAssistantConfig(): AssistantConfig {
  return {
    provider: 'openai',
    dashscope: { ...DEFAULT_DASHSCOPE_CONFIG },
    openai: { ...DEFAULT_OPENAI_CONFIG },
    gemini: { ...DEFAULT_GEMINI_CONFIG },
    anthropic: { ...DEFAULT_ANTHROPIC_CONFIG },
    generateOptions: { ...DEFAULT_GENERATE_OPTIONS },
  };
}

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

/** 助手配置 Tab 标识 */
export type AssistantTabKey = 'view' | 'llm';
