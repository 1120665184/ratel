package org.quyq.gwsu.common.security.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 数据资源断言类型
 *
 * @author Quyq
 * @date 2026/4/13
 */
@Getter
public enum DataResourceAssertType implements IEnum<String> {

    EQ("等于"),
    LIKE("模糊匹配");

    private final String description;

    DataResourceAssertType(String description) {
        this.description = description;
    }

    @JsonValue
    @Override
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static DataResourceAssertType fromValue(String value) {
        for (DataResourceAssertType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的断言类型: " + value);
    }
}
