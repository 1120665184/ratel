package org.quyq.gwsu.common.core.exception.errcode;


import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;

/**
 * @author Quyq
 * @date 2026/3/23
 * @description
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.COMMON_ERROR_CODE_MODULE, notes = "公共错误码")
public enum CommonErrorCode implements ReturnCode {
    E00000("服务器出错啦，请联系管理员"),
    E00001("资源未找到，请联系管理员"),

    E01001("获取数据库类型失败"),
    E01002("表名不能为空"),

    E03001("TOKEN已失效,请重新登录"),
    E03002("鉴权失败，请联系管理员"),
    E03003("缺少指定类型的数据资源条件构造器"),
    E03004("无头认证凭证失效，登录失败"),
    E03005("钉钉认证未配置'redirect_uri' ， 请联系管理员"),
    E03006("钉钉三方认证异常"),

    E04001("未知的登录类型"),
    E04002("该登录类型不支持生成web授权url"),
    E04003("三方登录缺少必要的配置信息"),
    E04004("登录类型不能为空"),
    E04005("登录终端类型不能为空"),
    E04006("工作区ID不能为空"),
    E04007("未知的工作区"),

    E05001("智能体会话存储失败"),
    E05002("智能体会话删除失败"),
    ;

    private final String msg;

    CommonErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
