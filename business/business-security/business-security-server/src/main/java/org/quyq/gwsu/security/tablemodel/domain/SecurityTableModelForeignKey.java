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

    @Schema(description = "字段名")
    private String columnName;

    @Schema(description = "引用表名")
    private String referencedTableName;

    @Schema(description = "引用字段名")
    private String referencedColumnName;

    @Schema(description = "数据类型：0-采集 1-自定义添加")
    private Integer dataType;

    @Schema(description = "备注")
    private String remark;

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
        vo.setColumnName(this.columnName);
        vo.setReferencedTableName(this.referencedTableName);
        vo.setReferencedColumnName(this.referencedColumnName);
        vo.setUpdateRule(this.updateRule);
        vo.setDeleteRule(this.deleteRule);
        vo.setDataType(this.dataType);
        vo.setRemark(this.remark);
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
        entity.setColumnName(vo.getColumnName());
        entity.setReferencedTableName(vo.getReferencedTableName());
        entity.setReferencedColumnName(vo.getReferencedColumnName());
        entity.setUpdateRule(vo.getUpdateRule());
        entity.setDeleteRule(vo.getDeleteRule());
        entity.setDataType(vo.getDataType());
        entity.setRemark(vo.getRemark());
        return entity;
    }
}
