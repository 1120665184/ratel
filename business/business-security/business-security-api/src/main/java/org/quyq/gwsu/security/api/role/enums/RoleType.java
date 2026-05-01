package org.quyq.gwsu.security.api.role.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 角色类型枚举
 *
 * @author Quyq
 */
@Getter
public enum RoleType {

    SYSTEM(1, "系统角色"),
    BUSINESS(2, "业务角色");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    RoleType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RoleType of(int code) {
        for (RoleType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
