package org.quyq.gwsu.security.apiresource.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.apiresource.vo.TableModelVO;
import org.quyq.gwsu.security.apiresource.typehandler.FieldConfigTypeHandler;

import java.util.Map;
import java.util.Objects;

/**
 * 接口-表模型绑定表（注解采集，启动时覆盖）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_api_table_model", autoResultMap = true)
@Schema(description = "接口-表模型绑定表")
public class SecurityApiTableModel extends BaseDO {

    @TableId(type = IdType.INPUT)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "接口资源ID")
    private String apiId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "表名")
    private String tableName;

    @TableField(typeHandler = FieldConfigTypeHandler.class)
    @Schema(description = "字段配置，key为字段名（下划线格式）")
    private Map<String, FieldPermission> fieldConfig;

    public TableModelVO toVo() {
        TableModelVO vo = new TableModelVO();
        vo.setId(this.id);
        vo.setApiId(this.apiId);
        vo.setModulePrefix(this.modulePrefix);
        vo.setDatasource(this.datasource);
        vo.setTableName(this.tableName);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityApiTableModel toDo(TableModelVO vo) {
        SecurityApiTableModel entity = new SecurityApiTableModel();
        entity.setId(vo.getId());
        entity.setApiId(vo.getApiId());
        entity.setModulePrefix(vo.getModulePrefix());
        entity.setDatasource(vo.getDatasource());
        entity.setTableName(vo.getTableName());
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SecurityApiTableModel that = (SecurityApiTableModel) o;
        return Objects.equals(id, that.id) && Objects.equals(apiId, that.apiId) && Objects.equals(modulePrefix, that.modulePrefix) && Objects.equals(datasource, that.datasource) && Objects.equals(tableName, that.tableName) && Objects.equals(fieldConfig, that.fieldConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, apiId, modulePrefix, datasource, tableName, fieldConfig);
    }
}
