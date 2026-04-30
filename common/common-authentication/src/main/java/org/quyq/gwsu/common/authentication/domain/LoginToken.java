package org.quyq.gwsu.common.authentication.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
@Data
public class LoginToken {

    @Schema(description = "用户ID")
    private String userId;

    /**
     * 返回给前端的告警，提示消息内容
     */
    @Schema(description = "告警消息内容")
    private String alterMsg;

    /**
     * token
     */
    @Schema(description = "登录token")
    private String token;

    /**
     * 过期时间
     */
    @Schema(description = "有效期（秒）")
    private Long expires;

    /**
     * 扩展数据
     */
    @Schema(description = "其他扩展数据")
    private Map<String, String> extraData;

}
