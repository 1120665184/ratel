package org.quyq.gwsu.security.headless.entrance.dingtalk.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Quyq
 * @date 2026/1/8
 * @description 钉钉用户
 */
@Data
public class DingTalkUser {

    @Schema(title = "用户ID")
    private String id;

    @Schema(title = "钉钉用户的unionId")
    private String unionId;

    @Schema(title = "姓名")
    private String name;

    @Schema(title = "手机号")
    private String mobile;

    @Schema(title = "职位")
    private String position;

    @Schema(title = "邮箱")
    private String email;

    @Schema(title = "办公地点")
    private String workPlace;

    @Schema(title = "扩展属性")
    private String extension;

    @Schema(title = "是否企业高管")
    private Boolean senior;

    @Schema(title = "是否为企业管理员")
    private Boolean admin;

    @Schema(title = "是否为企业老板")
    private Boolean boss;

    @Schema(title = "是否运维人员")
    private Boolean operations;

}
