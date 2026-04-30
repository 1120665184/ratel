package org.quyq.gwsu.system.api.manager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "重置密码请求")
public class ResetPasswordDTO {

    @Schema(description = "新密码")
    private String newPassword;
}
