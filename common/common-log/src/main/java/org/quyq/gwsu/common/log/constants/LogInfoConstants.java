package org.quyq.gwsu.common.log.constants;


/**
 * @author Quyq
 * @date 2026/5/21
 * @description 日志相关常量
 */
public interface LogInfoConstants {



    String TRACE_ID = "traceId";

    String SPAN_ID = "spanId";

    /**
     * 请求头中的traceId字段 格式：00-traceId-parentId-01(是否采样)
     */
    String HEADER_TRACE_INFO = "traceparent";


}
