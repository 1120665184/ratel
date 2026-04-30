package org.quyq.gwsu.security.dataresource.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.dataresource.vo.DataResourceVO;

/**
 * 数据资源配置主表
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "security_data_resource", autoResultMap = true)
@Schema(description = "数据资源配置主表")
public class SecurityDataResource extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "库名，为空时匹配所有库")
    private String databaseName;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "启用状态")
    private Boolean status;

    /**
     * 转换为 VO 对象
     */
    public DataResourceVO toVo() {
        DataResourceVO vo = new DataResourceVO();
        vo.setId(this.id);
        vo.setDatabaseName(this.databaseName);
        vo.setTableName(this.tableName);
        vo.setDescription(this.description);
        vo.setStatus(this.status);
        vo.copyBaseProperties(this);
        return vo;
    }

}
