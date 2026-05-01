package org.quyq.gwsu.security.api.role.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 有效期类型枚举
 *
 * @author Quyq
 */
@Getter
public enum ValidType {

    PERMANENT(1, "永久"),
    ABSOLUTE(2, "绝对时间范围"),
    CYCLE(3, "周期性");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    ValidType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ValidType of(int code) {
        for (ValidType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
