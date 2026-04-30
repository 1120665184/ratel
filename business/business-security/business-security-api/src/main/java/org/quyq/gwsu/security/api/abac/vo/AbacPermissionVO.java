package org.quyq.gwsu.security.api.abac.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;

/**
 * @author Quyq
 * @date 2026/4/4
 * @description abac接口访问权限信息
 */
@Data
public class AbacPermissionVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "表达式ID")
    private String abacId;

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "操作")
    private String action;

    @Schema(description = "URL模式")
    private String urlPattern;

    @Schema(description = "Permit/Deny")
    private AbacEffect effect;

    @Schema(description = "表达式内容")
    private String expression;
}
