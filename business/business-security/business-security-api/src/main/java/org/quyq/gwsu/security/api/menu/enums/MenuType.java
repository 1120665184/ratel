package org.quyq.gwsu.security.api.menu.enums;

import lombok.Getter;

/**
 * 菜单类型枚举
 *
 * @author Quyq
 */
@Getter
public enum MenuType {

    DIRECTORY(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    private final int code;
    private final String description;

    MenuType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MenuType of(int code) {
        for (MenuType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
