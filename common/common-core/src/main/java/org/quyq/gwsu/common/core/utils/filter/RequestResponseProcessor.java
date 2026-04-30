package org.quyq.gwsu.common.core.utils.filter;


import reactor.core.publisher.Mono;

/**
 * @author Quyq
 * @date 2026/4/1
 * @description
 */
public interface RequestResponseProcessor {

    /**
     * 前置处理
     * @param context
     * @return
     */
    default Mono<Boolean> preHandle(RequestResponseContext context) {
        return Mono.just(true);
    }

    /**
     * 后置处理
     * @param context
     * @return
     */
    default Mono<Void> postHandle(RequestResponseContext context) {
        return Mono.empty();
    }

    /**
     * 判断是否需要修改响应体
     * @param context 请求上下文（包含路径、头、参数等）
     * @return 是否需要响应体
     */
    default boolean needsResponseBody(RequestResponseContext context) {
        return false;
    }

}
