package org.quyq.gwsu.security.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Catalog信息")
public class SecurityCatalogVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Catalog唯一标识")
    private String catalogKey;

    @Schema(description = "Catalog名称")
    private String catalogName;

    @Schema(description = "Catalog描述")
    private String description;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "激活状态：0-未激活 1-激活")
    private Integer active;

    @Schema(description = "状态：0-禁用 1-正常")
    private Boolean status;
}
