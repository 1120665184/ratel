package org.quyq.gwsu.kit.job.scheduler.thread;

import java.util.List;

/**
 * Handler注册信息（内存缓存结构）
 *
 * @param handlerName handler名称
 * @param appname     执行器AppName（命名空间，防同名handler冲突）
 * @param addresses   在线地址列表
 */
public record HandlerRegistryInfo(String handlerName, String appname, List<String> addresses) {
}
