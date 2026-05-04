package org.quyq.gwsu.common.security.enums;

import lombok.Getter;

/**
 * 数据资源断言类型
 *
 * @author Quyq
 * @date 2026/4/13
 */
@Getter
public enum DataResourceAssertType {

    EQ("等于"),
    LIKE("模糊匹配");

    private final String description;

    DataResourceAssertType(String description) {
        this.description = description;
    }

}
