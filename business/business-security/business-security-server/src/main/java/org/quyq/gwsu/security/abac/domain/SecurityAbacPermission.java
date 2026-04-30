package org.quyq.gwsu.security.abac.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_abac_permission", autoResultMap = true)
@Schema(description = "ABAC接口权限关联表")
public class SecurityAbacPermission extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "表达式ID")
    private String abacId;

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "操作")
    private String action;

    @Schema(description = "URL模式")
    private String urlPattern;

    @Schema(description = "Permit/Deny")
    private AbacEffect effect;

    @Schema(description = "状态")
    private Boolean status;

}
