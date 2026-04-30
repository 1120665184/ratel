package org.quyq.gwsu.common.security.domain;


import lombok.Data;
import org.quyq.gwsu.common.security.enums.DataResourceAssertType;
import org.quyq.gwsu.common.security.enums.DataResourceFieldConditionType;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
@Data
public class DataResoureRule {

    /**
     * 库名
     */
    private String databaseName;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 字段拼接条件
     */
    private List<FieldCondition> conditions;


    @Data
    public static class FieldCondition {

        /**
         * 字段名
         */
        private String fieldName;

        /**
         * 显示过滤字段为null的数据,为 1时，会添加 field IS NULL 条件
         * 0: 不显示
         * 1: 显示
         */
        private boolean showNull;

        /**
         * 关联的对应用户数据资源字段
         */
        private List<String> userResourceFields;

        /**
         * 断言类型
         */
        private DataResourceAssertType assertType;


        /**
         * 与上一个字段条件的关联关系
         */
        private DataResourceFieldConditionType relationship;

    }

}
