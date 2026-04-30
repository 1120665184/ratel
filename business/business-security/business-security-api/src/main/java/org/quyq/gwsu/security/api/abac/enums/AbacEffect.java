package org.quyq.gwsu.security.api.abac.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @author Quyq
 * @date 2026/4/4
 * @description
 */
@Getter
public enum AbacEffect {
    /**
     * 允许
     */
    PERMIT("allow"),
    /**
     * 拒绝
     */
    DENY("deny");

    @EnumValue
    @JsonValue
    private final String effect;

    private AbacEffect(String effect) {
        this.effect = effect;
    }

}
