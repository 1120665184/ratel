package org.quyq.gwsu.security.api.role.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 数据范围枚举
 *
 * @author Quyq
 */
@Getter
public enum DataScope {

    ALL(0, "全部数据"),
    CUSTOM(1, "自定义"),
    DEPT_AND_BELOW(2, "本部门及以下"),
    DEPT_ONLY(3, "本部门"),
    SELF_ONLY(4, "仅本人");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    DataScope(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DataScope of(int code) {
        for (DataScope scope : values()) {
            if (scope.getCode() == code) {
                return scope;
            }
        }
        return null;
    }
}
