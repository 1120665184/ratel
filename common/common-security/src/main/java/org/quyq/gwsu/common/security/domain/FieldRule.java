package org.quyq.gwsu.common.security.domain;



import java.util.Set;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description 字段规则
 */
public record FieldRule(
        /**
         * 接口
         */
        String url,
        /**
         * allow: 允许展示
         * deny: 禁止展示
         */
        String effect ,
        /**
         * 对应字段
         */
        Set<String> fields,
        /**
         * 表达式
         */
        String expression
) { }
