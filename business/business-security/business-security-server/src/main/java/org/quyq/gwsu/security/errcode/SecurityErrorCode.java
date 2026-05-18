package org.quyq.gwsu.security.errcode;


import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

/**
 * 安全模块错误码
 *
 * @author Quyq
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.SECURITY_ERROR_CODE_MODULE, notes = "安全模块错误码")
public enum SecurityErrorCode implements ReturnCode {

    E01001("无效的菜单位置类型"),
    E01002("无效的菜单所属类型"),
    E01003("菜单名称不能为空"),
    E01004("按钮标识不能为空"),
    E01005("功能描述不能为空"),


    E02001("角色不存在"),
    E02002("角色编码已存在"),
    E02003("系统角色不可删除"),
    E02004("角色已禁用"),
    E02005("时效配置无效"),
    E02006("该时效分组已存在，请编辑已有配置"),

    E03001("表名称不能为空"),
    E03002("数据源不能为空"),
    E03003("角色ID不能为空"),
    E03004("所属服务（模块）不能为空") ,

    E04001("所属服务不能为空") ,
    E04002("表名不能为空"),
    ;

    private final String msg;

    SecurityErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
