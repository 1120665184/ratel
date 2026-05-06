import type { WebToolExecutor } from './types';

/** 工具注册表 */
const registry = new Map<string, WebToolExecutor>();

/**
 * 注册Web工具执行器
 * @param name 工具名称，需与后端 @Tool 注解的名称一致
 * @param executor 工具执行器
 */
export function registerWebTool(name: string, executor: WebToolExecutor): void {
  registry.set(name, executor);
}

/**
 * 获取已注册的工具执行器
 * @param name 工具名称
 * @returns 工具执行器，未注册返回 undefined
 */
export function getWebTool(name: string): WebToolExecutor | undefined {
  return registry.get(name);
}
