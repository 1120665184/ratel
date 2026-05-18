package org.quyq.gwsu.security.tablemodel.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;

/**
 * 表基本信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_tablemodel_tables", autoResultMap = true)
@Schema(description = "表基本信息")
public class SecurityTableModelTable extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源")
    private String dataSource;

    @Schema(description = "表注释")
    private String tableComment;

    @Schema(description = "来源类型：0-采集 1-自定义添加")
    private Integer sourceType;

    /**
     * DO 转 VO
     *
     * @return TableModelTableVO
     */
    public TableModelTableVO toVo() {
        TableModelTableVO vo = new TableModelTableVO();
        vo.setId(this.id);
        vo.setTableName(this.tableName);
        vo.setModulePrefix(this.modulePrefix);
        vo.setDataSource(this.dataSource);
        vo.setTableComment(this.tableComment);
        vo.setSourceType(this.sourceType);
        vo.copyBaseProperties(this);
        return vo;
    }

    /**
     * VO 转 DO
     *
     * @param vo 表信息VO
     * @return SecurityTableModelTable
     */
    public static SecurityTableModelTable toDo(TableModelTableVO vo) {
        SecurityTableModelTable entity = new SecurityTableModelTable();
        entity.setId(vo.getId());
        entity.setTableName(vo.getTableName());
        entity.setModulePrefix(vo.getModulePrefix());
        entity.setDataSource(vo.getDataSource());
        entity.setTableComment(vo.getTableComment());
        entity.setSourceType(vo.getSourceType());
        return entity;
    }
}
