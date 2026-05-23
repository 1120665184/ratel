package org.quyq.gwsu.security.catalog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_catalog_component")
@Schema(description = "Catalog组件配置表")
public class SecurityCatalogComponent extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "组件名")
    private String componentName;

    @Schema(description = "组件描述（给AI看的）")
    private String description;

    @Schema(description = "组件属性JSON Schema定义")
    private String propsSchema;

    @Schema(description = "默认属性值")
    private String defaultProps;

    @Schema(description = "组件分类（display/chart/form）")
    private String category;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    public SecurityCatalogComponentVO toVo() {
        SecurityCatalogComponentVO vo = new SecurityCatalogComponentVO();
        vo.setId(this.id);
        vo.setComponentName(this.componentName);
        vo.setDescription(this.description);
        vo.setPropsSchema(this.propsSchema);
        vo.setDefaultProps(this.defaultProps);
        vo.setCategory(this.category);
        vo.setSortOrder(this.sortOrder);
        vo.setStatus(this.status != null && this.status == 1);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityCatalogComponent toDo(SecurityCatalogComponentVO vo) {
        SecurityCatalogComponent entity = new SecurityCatalogComponent();
        entity.setId(vo.getId());
        entity.setComponentName(vo.getComponentName());
        entity.setDescription(vo.getDescription());
        entity.setPropsSchema(vo.getPropsSchema());
        entity.setDefaultProps(vo.getDefaultProps());
        entity.setCategory(vo.getCategory());
        entity.setSortOrder(vo.getSortOrder());
        entity.setStatus(Boolean.TRUE.equals(vo.getStatus()) ? 1 : 0);
        return entity;
    }
}
