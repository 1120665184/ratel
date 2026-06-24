package org.quyq.gwsu.system.api.manager.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可绑定的钉钉账号选项
 *
 * @author Quyq
 */
@Data
@Schema(description = "可绑定的钉钉账号选项")
public class DingTalkAccountOptionVO {

    @Schema(description = "账号ID")
    private String id;

    @Schema(description = "登录标识（钉钉unionId）")
    private String identifier;

    @Schema(description = "所属用户ID")
    private String userId;

    @Schema(description = "所属用户昵称")
    private String nickname;

    @Schema(description = "所属用户名")
    private String userName;
}
