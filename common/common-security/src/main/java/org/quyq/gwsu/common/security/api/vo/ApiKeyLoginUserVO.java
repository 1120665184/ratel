package org.quyq.gwsu.common.security.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API_KEY 登录用户信息
 *
 * @author Quyq
 */
@Data
@Schema(description = "API_KEY 登录用户信息")
public class ApiKeyLoginUserVO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "API_KEY ID")
    private String apiKeyId;

    @Schema(description = "登录类型")
    private String loginType;
}
