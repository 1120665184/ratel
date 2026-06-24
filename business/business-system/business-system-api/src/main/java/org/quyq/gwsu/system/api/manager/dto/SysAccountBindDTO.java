package org.quyq.gwsu.system.api.manager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "账号绑定请求")
public class SysAccountBindDTO {

    @Schema(description = "登录类型：password-密码 phone-手机 dingtalk-钉钉")
    private String identityType;

    @Schema(description = "登录标识")
    private String identifier;

    @Schema(description = "凭证/密码")
    private String credential;

    @Schema(description = "绑定时原钉钉账号所属用户ID（用于切换绑定，删除原用户）")
    private String originalUserId;
}
