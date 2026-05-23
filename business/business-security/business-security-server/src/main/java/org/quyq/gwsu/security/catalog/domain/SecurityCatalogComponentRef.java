package org.quyq.gwsu.security.catalog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_catalog_component_ref")
@Schema(description = "Catalog与组件关联表")
public class SecurityCatalogComponentRef extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Catalog ID")
    private String catalogId;

    @Schema(description = "组件ID")
    private String componentId;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
