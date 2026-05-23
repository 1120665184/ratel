package org.quyq.gwsu.security.catalog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogVO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_catalog")
@Schema(description = "Catalog定义表")
public class SecurityCatalog extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
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
    private Integer status;

    public SecurityCatalogVO toVo() {
        SecurityCatalogVO vo = new SecurityCatalogVO();
        vo.setId(this.id);
        vo.setCatalogKey(this.catalogKey);
        vo.setCatalogName(this.catalogName);
        vo.setDescription(this.description);
        vo.setVersion(this.version);
        vo.setActive(this.active);
        vo.setStatus(this.status != null && this.status == 1);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityCatalog toDo(SecurityCatalogVO vo) {
        SecurityCatalog entity = new SecurityCatalog();
        entity.setId(vo.getId());
        entity.setCatalogKey(vo.getCatalogKey());
        entity.setCatalogName(vo.getCatalogName());
        entity.setDescription(vo.getDescription());
        entity.setVersion(vo.getVersion());
        entity.setActive(vo.getActive());
        entity.setStatus(Boolean.TRUE.equals(vo.getStatus()) ? 1 : 0);
        return entity;
    }
}
