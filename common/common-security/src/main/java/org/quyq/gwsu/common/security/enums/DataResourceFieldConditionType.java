package org.quyq.gwsu.common.security.enums;

import lombok.Getter;

/**
 * 数据资源字段条件关联关系
 *
 * @author Quyq
 * @date 2026/4/10
 */
@Getter
public enum DataResourceFieldConditionType {

    AND("与"),
    OR("或");

    private final String description;

    DataResourceFieldConditionType(String description) {
        this.description = description;
    }

}
