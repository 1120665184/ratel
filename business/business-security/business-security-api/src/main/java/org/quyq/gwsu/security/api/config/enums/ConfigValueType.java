package org.quyq.gwsu.security.api.config.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description
 */
@Getter
public enum ConfigValueType {
    STR(1),
    NUMBER(2),
    BOOL(3),
    JSON(4)
    ;
    @EnumValue
    @JsonValue
    private final int value;
    ConfigValueType(int value) {
        this.value = value;
    }


    public static ConfigValueType from(int value) {
       return  Arrays.stream(ConfigValueType.values())
               .filter(v ->v.value == value).findFirst().orElse(null);
    }

}
