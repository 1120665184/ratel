package org.quyq.gwsu.security.abac.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_abac", autoResultMap = true)
@Schema(description = "ABAC表达式表")
public class SecurityAbac extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "表达式内容")
    private String expression;

    @Schema(description = "表达式描述")
    private String description;

    @Schema(description = "启用状态")
    private Boolean status;

}
