package org.quyq.gwsu.common.authentication.domain;


import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
@Data
public class LoginVO {

    private String userId;

    /**
     * 返回给前端的告警，提示消息内容
     */
    private String alterMsg;

    /**
     * 登陆token
     */
    private String token;

    /**
     * 过期时间
     */
    private Long expires;

    /**
     * 执行异常时的错误信息，用于重定向方式的数据返回
     */
    private String errMsg;
    private String errCode;

    /**
     * 是否需要重定向
     */
    private boolean needRedirect;

    /**
     * 重定向URL
     */
    private String redirectUrl;

    /**
     * 扩展数据
     */
    private final Map<String, String> extraData = new HashMap<>();

    /**
     * 转换为TOKEN
     * @return
     */
    public LoginToken conversionToken() {
        LoginToken loginToken = new LoginToken();
        loginToken.setUserId(userId);
        loginToken.setAlterMsg(alterMsg);
        loginToken.setToken(token);
        loginToken.setExpires(expires);
        loginToken.setExtraData(extraData);
        return loginToken;
    }

    public boolean isFail(){
        return StringUtils.hasText(errMsg) || StringUtils.hasText(errCode);
    }

}
