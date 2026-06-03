package org.quyq.gwsu.common.log.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author Quyq
 * @date 2026/6/3
 * @description 界面操作主体
 */
@Getter
public enum ViewOperationSubject {
    /**
     * 人类
     */
    HUMAN(0) ,
    /**
     * 智能助手
     */
    ASSISTANT(1)
    ;

    @JsonValue
    @EnumValue
    private final int type;

    ViewOperationSubject(int type) {
        this.type = type;
    }

    public static ViewOperationSubject getViewOperationSubject(int type) {
        return  Arrays.stream(ViewOperationSubject.values())
                .filter(v -> v.type == type).findFirst().orElse(HUMAN);
    }

}
