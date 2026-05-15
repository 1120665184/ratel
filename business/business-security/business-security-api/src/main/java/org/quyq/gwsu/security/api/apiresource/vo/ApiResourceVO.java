package org.quyq.gwsu.security.api.apiresource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

/**
 * 接口资源信息
 *
 * @author Quyq
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "接口资源信息")
public class ApiResourceVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "Tag标签名称")
    private String tagName;

    @Schema(description = "接口地址")
    private String reqPath;

    @Schema(description = "请求方式(GET/POST/PUT/DELETE等)")
    private String reqMethod;

    @Schema(description = "接口摘要")
    private String summary;

    @Schema(description = "登录后允许访问")
    public Boolean loginAllowAccess;
}
