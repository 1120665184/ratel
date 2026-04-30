package org.quyq.gwsu.security.api.menu.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 菜单所属类型枚举
 *
 * @author Quyq
 */
@Getter
public enum MenuOwner {

    ADMIN(1, "后端管理"),
    APP(2, "移动端APP");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    MenuOwner(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MenuOwner of(int code) {
        for (MenuOwner owner : values()) {
            if (owner.getCode() == code) {
                return owner;
            }
        }
        return null;
    }
}
