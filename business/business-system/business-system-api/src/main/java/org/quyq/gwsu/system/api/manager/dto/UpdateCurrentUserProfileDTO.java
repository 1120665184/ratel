package org.quyq.gwsu.system.api.manager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前登录用户资料更新请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "当前登录用户资料更新请求")
public class UpdateCurrentUserProfileDTO {

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;
}
