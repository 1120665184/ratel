package org.quyq.gwsu.security.api.apiresource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 接口资源查询条件
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "接口资源查询条件")
public class ApiResourceQueryDTO extends BaseDTO {

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "Tag标签名称")
    private String tagName;

    @Schema(description = "接口地址（模糊查询）")
    private String reqPath;

    @Schema(description = "请求方式")
    private String reqMethod;

    @Schema(description = "关键词（同时模糊搜索Tag名称、接口地址、摘要）")
    private String keyword;
}
