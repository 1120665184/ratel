package org.quyq.gwsu.security.tablemodel.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelForeignKeyVO;

/**
 * 外键约束信息（单列外键）
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_tablemodel_foreign_keys", autoResultMap = true)
@Schema(description = "外键约束信息（单列外键）")
public class SecurityTableModelForeignKey extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "约束名称")
    private String constraintName;

    @Schema(description = "所属表ID")
    private String tableId;

    @Schema(description = "字段ID")
    private String columnId;

    @Schema(description = "引用表ID")
    private String referencedTableId;

    @Schema(description = "引用字段ID")
    private String referencedColumnId;

    @Schema(description = "更新规则")
    private String updateRule;

    @Schema(description = "删除规则")
    private String deleteRule;

    /**
     * DO 转 VO
     *
     * @return TableModelForeignKeyVO
     */
    public TableModelForeignKeyVO toVo() {
        TableModelForeignKeyVO vo = new TableModelForeignKeyVO();
        vo.setId(this.id);
        vo.setConstraintName(this.constraintName);
        vo.setTableId(this.tableId);
        vo.setColumnId(this.columnId);
        vo.setReferencedTableId(this.referencedTableId);
        vo.setReferencedColumnId(this.referencedColumnId);
        vo.setUpdateRule(this.updateRule);
        vo.setDeleteRule(this.deleteRule);
        vo.copyBaseProperties(this);
        return vo;
    }

    /**
     * VO 转 DO
     *
     * @param vo 外键信息VO
     * @return SecurityTableModelForeignKey
     */
    public static SecurityTableModelForeignKey toDo(TableModelForeignKeyVO vo) {
        SecurityTableModelForeignKey entity = new SecurityTableModelForeignKey();
        entity.setId(vo.getId());
        entity.setConstraintName(vo.getConstraintName());
        entity.setTableId(vo.getTableId());
        entity.setColumnId(vo.getColumnId());
        entity.setReferencedTableId(vo.getReferencedTableId());
        entity.setReferencedColumnId(vo.getReferencedColumnId());
        entity.setUpdateRule(vo.getUpdateRule());
        entity.setDeleteRule(vo.getDeleteRule());
        return entity;
    }
}
