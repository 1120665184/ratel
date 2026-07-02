package org.quyq.gwsu.system.api.manager.vo;


import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Quyq
 * @date 2026/7/2
 * @description 登录配置信息
 */
public record LoginInfoVO(
        @Schema(description = "项目名")
        String projectName
) {
}
