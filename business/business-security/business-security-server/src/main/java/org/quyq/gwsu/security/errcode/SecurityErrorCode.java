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
