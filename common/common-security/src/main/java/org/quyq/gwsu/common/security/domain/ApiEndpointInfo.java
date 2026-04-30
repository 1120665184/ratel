package org.quyq.gwsu.common.security.domain;

/**
 * HTTP 接口信息
 *
 * @param modulePrefix  所属模块前缀
 * @param tagName       Tag 标签名称
 * @param reqPath       接口地址
 * @param reqMethod     请求方式 (GET/POST/PUT/DELETE等)
 * @param summary       Operation 摘要
 * @param requestClass  请求参数类型全限定名
 * @param responseClass 响应类型全限定名
 */
public record ApiEndpointInfo(
        String id,
        String modulePrefix,
        String tagName,
        String reqPath,
        String reqMethod,
        String summary,
        String requestClass,
        String responseClass,
        String className ,
        String methodName ,
        boolean allowLoginAccess
) {
}
