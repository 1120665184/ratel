package org.quyq.gwsu.system.manager.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;
import org.quyq.gwsu.common.security.annotation.TableModelField;
import org.quyq.gwsu.system.api.manager.vo.UserVO;

import java.time.LocalDateTime;

/**
 * 用户表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "sys_user", autoResultMap = true)
@Schema(description = "用户表")
public class SysUser extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "邮箱")
    @TableModelField(desensitize = true ,strategy = SensitiveStrategy.EMAIL)
    private String email;

    @Schema(description = "手机号")
    @TableModelField(desensitize = true ,strategy = SensitiveStrategy.PHONE)
    private String phone;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    /**
     * DO 转 VO
     *
     * @return UserVO
     */
    public UserVO toVo() {
        UserVO vo = new UserVO();
        vo.setUserId(this.id);
        vo.setUserName(this.username);
        vo.setNickname(this.nickname);
        vo.setAvatar(this.avatar);
        vo.setEmail(this.email);
        vo.setPhone(this.phone);
        vo.setGender(this.gender);
        vo.setStatus(this.status);
        vo.setLastLoginTime(this.lastLoginTime);
        vo.copyBaseProperties(this);
        return vo;
    }

    /**
     * VO 转 DO
     *
     * @param vo UserVO
     * @return SysUser
     */
    public static SysUser toDo(UserVO vo) {
        SysUser user = new SysUser();
        user.setId(vo.getUserId());
        user.setUsername(vo.getUserName());
        user.setNickname(vo.getNickname());
        user.setAvatar(vo.getAvatar());
        user.setEmail(vo.getEmail());
        user.setPhone(vo.getPhone());
        user.setGender(vo.getGender());
        user.setStatus(vo.getStatus());
        return user;
    }
}
