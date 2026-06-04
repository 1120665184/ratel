package org.quyq.gwsu.log.errcode;

import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

/**
 * 日志模块错误码
 *
 * @author Quyq
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.LOG_ERROR_CODE_MODULE, notes = "日志模块错误码")
public enum LogErrorCode implements ReturnCode {

    E01001("操作日志保存失败"),
    E01002("操作日志不存在"),
    E01003("操作日志ID不能为空"),

    ;

    private final String msg;

    LogErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
