package org.quyq.gwsu.common.security.domain;


import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description 请求上下文
 */
public record RequestContext(
        /**
         * 主体
         */
        Subject<?> subject ,
        /**
         * 资源类型
         */
        String resType ,
        /**
         * 资源
         */
        String resUrl ,
        /**
         * 操作
         */
        String action ,

        /**
         * 环境
         */
        Map<String, Object> env

) {
}
