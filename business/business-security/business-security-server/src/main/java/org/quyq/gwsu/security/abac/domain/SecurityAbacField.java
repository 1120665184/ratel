package org.quyq.gwsu.security.abac.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;
import org.quyq.gwsu.security.api.typehandler.StringListTypeHandler;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_abac_field", autoResultMap = true)
@Schema(description = "ABAC字段权限关联表")
public class SecurityAbacField extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "表达式ID")
    private String abacId;

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "操作")
    private String action;

    @Schema(description = "URL")
    private String urlPattern;

    @Schema(description = "Allow/Deny")
    private AbacEffect fieldMode;

    @Schema(description = "字段")
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> fields;

    @Schema(description = "状态")
    private Boolean status;

}
