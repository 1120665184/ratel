package org.quyq.gwsu.security.api.menu.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 菜单位置类型枚举
 *
 * @author Quyq
 */
@Getter
public enum MenuPosition {

    SIDEBAR(1, "侧边栏"),
    HEADER(2, "顶部栏");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    MenuPosition(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MenuPosition of(int code) {
        for (MenuPosition position : values()) {
            if (position.getCode() == code) {
                return position;
            }
        }
        return null;
    }
}
