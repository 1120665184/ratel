package org.quyq.gwsu.common.security.domain;

import java.util.Map;

/**
 * 表模型信息
 *
 * @param modulePrefix 模块前缀
 * @param tableName    表名
 * @param datasource   数据源名称
 * @param fieldConfig  字段配置，key为字段名（下划线格式）
 */
public record TableModelInfo(
        String modulePrefix,
        String tableName,
        String datasource,
        Map<String, FieldPermission> fieldConfig
) {
    /**
     * 表模型唯一标识：module_prefix:datasource:table_name
     */
    public String uniqueKey() {
        return modulePrefix + ":" + datasource + ":" + tableName;
    }
}
