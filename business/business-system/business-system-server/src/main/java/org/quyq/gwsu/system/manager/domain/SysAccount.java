package org.quyq.gwsu.system.manager.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.annotation.TableModelField;
import org.quyq.gwsu.system.api.manager.vo.AccountVO;

import java.time.LocalDateTime;

/**
 * 账号表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "sys_account", autoResultMap = true)
@Schema(description = "账号表")
public class SysAccount extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "关联用户ID")
    private String userId;

    @Schema(description = "登录类型，与LoginHandler.loginType()匹配")
    private String identityType;

    @Schema(description = "登录标识")
    private String identifier;

    @Schema(description = "凭证")
    @TableModelField(show = false)
    private String credential;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    @Schema(description = "是否已验证")
    private Boolean verified;

    @Schema(description = "验证时间")
    private LocalDateTime verifiedTime;

    @Schema(description = "绑定时间")
    private LocalDateTime bindTime;

    /**
     * DO 转 VO
     *
     * @return AccountVO
     */
    public AccountVO toVo() {
        AccountVO vo = new AccountVO();
        vo.setId(this.id);
        vo.setUserId(this.userId);
        vo.setIdentityType(this.identityType);
        vo.setIdentifier(this.identifier);
        vo.setStatus(this.status);
        vo.setVerified(this.verified);
        vo.setVerifiedTime(this.verifiedTime);
        vo.setBindTime(this.bindTime);
        vo.copyBaseProperties(this);
        return vo;
    }
}
