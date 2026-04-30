package org.quyq.gwsu.security.api.role.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

/**
 * 主体角色关联信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "主体角色关联信息")
public class RoleSubjectVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "主体ID（用户ID）")
    private String subjectId;

    @Schema(description = "角色ID")
    private String roleId;

}
