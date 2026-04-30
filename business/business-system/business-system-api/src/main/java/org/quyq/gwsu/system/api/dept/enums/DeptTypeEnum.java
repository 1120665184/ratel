package org.quyq.gwsu.system.api.dept.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 部门类型枚举
 *
 * @author Quyq
 */
@Getter
@AllArgsConstructor
public enum DeptTypeEnum {

    COMPANY(1, "公司"),
    BRANCH(2, "分公司"),
    DEPARTMENT(3, "部门"),
    GROUP(4, "小组"),
    VIRTUAL_TEAM(5, "虚拟团队");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    /**
     * 根据编码获取枚举
     */
    public static DeptTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeptTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}