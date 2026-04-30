package org.quyq.gwsu.security.role.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.role.vo.RoleSubjectVO;

/**
 * 主体角色关联表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_role_subject", autoResultMap = true)
@Schema(description = "主体角色关联表")
public class SecurityRoleSubject extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "主体ID（用户ID）")
    private String subjectId;

    @Schema(description = "角色ID")
    private String roleId;

    /**
     * DO 转 VO
     *
     * @return SubjectRoleVO
     */
    public RoleSubjectVO toVo() {
        RoleSubjectVO vo = new RoleSubjectVO();
        vo.setId(this.id);
        vo.setSubjectId(this.subjectId);
        vo.setRoleId(this.roleId);
        vo.copyBaseProperties(this);
        return vo;
    }
}
