package org.quyq.gwsu.security.tablemodel.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelColumnVO;

/**
 * 字段详细信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_tablemodel_columns", autoResultMap = true)
@Schema(description = "字段详细信息")
public class SecurityTableModelColumn extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "关联表ID")
    private String tableId;

    @Schema(description = "字段名")
    private String columnName;

    @Schema(description = "字段类型")
    private String columnType;

    @Schema(description = "字段长度")
    private Integer columnLength;

    @Schema(description = "字段精度")
    private Integer columnScale;

    @Schema(description = "是否可空：true-是 false-否")
    private Boolean isNullable;

    @Schema(description = "是否主键：true-是 false-否")
    private Boolean isPrimaryKey;

    @Schema(description = "主键位置")
    private Integer pkPosition;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "字段注释")
    private String columnComment;

    @Schema(description = "字段顺序")
    private Integer ordinalPosition;

    /**
     * DO 转 VO
     *
     * @return TableModelColumnVO
     */
    public TableModelColumnVO toVo() {
        TableModelColumnVO vo = new TableModelColumnVO();
        vo.setId(this.id);
        vo.setTableId(this.tableId);
        vo.setColumnName(this.columnName);
        vo.setColumnType(this.columnType);
        vo.setColumnLength(this.columnLength);
        vo.setColumnScale(this.columnScale);
        vo.setIsNullable(this.isNullable);
        vo.setIsPrimaryKey(this.isPrimaryKey);
        vo.setPkPosition(this.pkPosition);
        vo.setDefaultValue(this.defaultValue);
        vo.setColumnComment(this.columnComment);
        vo.setOrdinalPosition(this.ordinalPosition);
        vo.copyBaseProperties(this);
        return vo;
    }

    /**
     * VO 转 DO
     *
     * @param vo 字段信息VO
     * @return SecurityTableModelColumn
     */
    public static SecurityTableModelColumn toDo(TableModelColumnVO vo) {
        SecurityTableModelColumn entity = new SecurityTableModelColumn();
        entity.setId(vo.getId());
        entity.setTableId(vo.getTableId());
        entity.setColumnName(vo.getColumnName());
        entity.setColumnType(vo.getColumnType());
        entity.setColumnLength(vo.getColumnLength());
        entity.setColumnScale(vo.getColumnScale());
        entity.setIsNullable(vo.getIsNullable());
        entity.setIsPrimaryKey(vo.getIsPrimaryKey());
        entity.setPkPosition(vo.getPkPosition());
        entity.setDefaultValue(vo.getDefaultValue());
        entity.setColumnComment(vo.getColumnComment());
        entity.setOrdinalPosition(vo.getOrdinalPosition());
        return entity;
    }
}
