package org.quyq.gwsu.security.api.abac.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description abac字段权限信息
 */
@Data
public class AbacFieldVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "表达式ID")
    private String abacId;

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "操作")
    private String action;

    @Schema(description = "URL")
    private String urlPattern;

    @Schema(description = "Allow/Deny")
    private AbacEffect fieldMode;

    @Schema(description = "字段名称")
    private List<String> fields;

    @Schema(description = "表达式内容")
    private String expression;


}
