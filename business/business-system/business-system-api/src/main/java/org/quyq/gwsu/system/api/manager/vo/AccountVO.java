package org.quyq.gwsu.system.api.manager.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.time.LocalDateTime;

/**
 * 账号 VO
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "账号信息")
public class AccountVO extends BaseVO {

    @Schema(description = "账号ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "登录类型")
    private String identityType;

    @Schema(description = "登录标识")
    private String identifier;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    @Schema(description = "是否已验证")
    private Boolean verified;

    @Schema(description = "验证时间")
    private LocalDateTime verifiedTime;

    @Schema(description = "绑定时间")
    private LocalDateTime bindTime;
}
