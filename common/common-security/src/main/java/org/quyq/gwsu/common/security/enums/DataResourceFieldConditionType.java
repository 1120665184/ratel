package org.quyq.gwsu.common.security.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 数据资源字段条件关联关系
 *
 * @author Quyq
 * @date 2026/4/10
 */
@Getter
public enum DataResourceFieldConditionType implements IEnum<String> {

    AND("与"),
    OR("或");

    private final String description;

    DataResourceFieldConditionType(String description) {
        this.description = description;
    }

    @JsonValue
    @Override
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static DataResourceFieldConditionType fromValue(String value) {
        for (DataResourceFieldConditionType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的条件关联关系: " + value);
    }
}
