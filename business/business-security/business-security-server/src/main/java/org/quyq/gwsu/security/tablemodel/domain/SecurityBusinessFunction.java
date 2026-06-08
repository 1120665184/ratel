package org.quyq.gwsu.security.tablemodel.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionVO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_business_function", autoResultMap = true)
@Schema(description = "AI业务功能配置")
public class SecurityBusinessFunction extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "业务名称")
    private String name;

    @Schema(description = "业务简介")
    private String summary;

    @Schema(description = "详细介绍（Markdown格式）")
    private String detail;

    @Schema(description = "排序号")
    private Integer sortOrder;

    public BusinessFunctionVO toVo() {
        BusinessFunctionVO vo = new BusinessFunctionVO();
        vo.setId(this.id);
        vo.setName(this.name);
        vo.setSummary(this.summary);
        vo.setDetail(this.detail);
        vo.setSortOrder(this.sortOrder);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityBusinessFunction toDo(BusinessFunctionVO vo) {
        SecurityBusinessFunction entity = new SecurityBusinessFunction();
        entity.setId(vo.getId());
        entity.setName(vo.getName());
        entity.setSummary(vo.getSummary());
        entity.setDetail(vo.getDetail());
        entity.setSortOrder(vo.getSortOrder());
        return entity;
    }
}
