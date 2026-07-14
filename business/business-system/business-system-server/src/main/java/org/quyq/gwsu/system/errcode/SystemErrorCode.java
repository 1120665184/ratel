package org.quyq.gwsu.system.errcode;


import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.SYSTEM_ERROR_CODE_MODULE, notes = "系统模块错误码")
public enum SystemErrorCode implements ReturnCode {

    E00001("用户名或密码错误"),
    E00002("账号已被禁用"),
    E00003("用户不存在"),
    E00004("用户已被禁用"),

    // 部门模块错误码 E01xxx
    E01001("部门不存在"),
    E01002("部门名称重复"),
    E01003("部门存在子部门，无法删除"),
    E01004("部门存在关联用户，无法删除"),
    E01005("父部门不存在"),
    E01006("父部门不能是自己"),
    E01007("父部门形成循环引用"),
    E01008("部门类型不可修改"),
    E01009("主父部门不能通过此接口移除"),

    E01010("用户部门关联不存在"),
    E01011("用户已在该部门"),
    E01012("用户必须有一个主部门"),
    E01013("主部门不在部门列表中"),
    E01014("移除主部门时需指定新主部门"),

    // 用户模块错误码 E02xxx
    E02001("用户名已存在"),
    E02002("手机号已被绑定"),
    E02003("邮箱已被使用"),
    E02004("至少保留一个登录账号"),
    E02005("用户不存在"),
    E02006("用户名不能为空"),
    E02007("初始密码不能为空"),
    E02008("用户无密码账号，无法修改密码"),
    E02009("新密码不能为空"),
    E02010("旧密码不能为空"),
    E02011("旧密码错误"),
    E02012("昵称不能为空"),
    E02013("性别参数不合法"),

    // API_KEY 模块错误码 E03xxx
    E03001("API_KEY 名称不能为空"),
    E03002("API_KEY 名称长度不能超过128个字符"),
    E03003("API_KEY 有效期类型不能为空"),
    E03004("API_KEY 有效天数必须大于0"),
    E03005("API_KEY 过期时间必须晚于当前时间"),
    E03006("API_KEY 不存在"),
    E03007("无权操作该 API_KEY"),
    E03008("API_KEY 已失效"),
    E03009("API_KEY 已停用"),

    ;

    private final String msg;

    SystemErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
