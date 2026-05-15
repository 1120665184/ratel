package org.quyq.gwsu.security.tablemodel.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelConfigVO;

/**
 * 表模型手动配置表（持久化，启动不覆盖）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_api_table_model_config", autoResultMap = true)
@Schema(description = "表模型手动配置表")
public class SecurityApiTableModelConfig extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "关联的接口-表模型绑定ID")
    private String tableModelId;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "配置说明")
    private String description;

    public TableModelConfigVO toVo() {
        TableModelConfigVO vo = new TableModelConfigVO();
        vo.setId(this.id);
        vo.setTableModelId(this.tableModelId);
        vo.setTableName(this.tableName);
        vo.setModulePrefix(this.modulePrefix);
        vo.setDatasource(this.datasource);
        vo.setDescription(this.description);
        vo.copyBaseProperties(this);
        return vo;
    }
}
