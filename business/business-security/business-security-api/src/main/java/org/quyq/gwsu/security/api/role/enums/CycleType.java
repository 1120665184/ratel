package org.quyq.gwsu.security.api.role.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 周期类型枚举
 *
 * @author Quyq
 */
@Getter
public enum CycleType {

    WEEKLY(1, "按周"),
    MONTHLY(2, "按月");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    CycleType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CycleType of(int code) {
        for (CycleType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
