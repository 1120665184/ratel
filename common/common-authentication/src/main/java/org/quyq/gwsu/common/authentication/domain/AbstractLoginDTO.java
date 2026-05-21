package org.quyq.gwsu.common.authentication.domain;


import lombok.Data;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.springframework.util.MultiValueMap;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
@Data
public abstract class AbstractLoginDTO {

    /**
     * 登录类型
     */
    private String type;


    /**
     * 登录终端类型
     * 1-PC ; 2-APP
     */
    private TerminalType terminal;


    /**
     * 额外扩展参数
     */
    private MultiValueMap<String, String> extraParam;

}
