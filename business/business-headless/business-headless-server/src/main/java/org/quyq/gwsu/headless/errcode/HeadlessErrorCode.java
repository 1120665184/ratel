package org.quyq.gwsu.headless.errcode;


import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

/**
 * @author Quyq
 * @date 2026/6/25
 * @description
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.HEADLESS_ERROR_CODE_MODULE, notes = "无头智能体模块错误码")
public enum HeadlessErrorCode implements ReturnCode {

    E01001("路由信息数据为空,执行失败！"),
    E01002("审批信息数据为空，执行失败！"),
    E01003("用户回复信息数据为空，执行失败！"),
    E01004("toolCallId数据为空，执行失败！"),
    E01005("用户ID不能为空"),
    E01006("用户回复不能为空"),
    E01007("无头智能体请求不能为空"),
    E01008("无头智能体资源参数不能为空"),
    E01009("无头智能体资源fileId不能为空"),
    E01010("未找到对应的文件信息"),
    E01011("文件媒体类型不能为空"),

    ;

    private final String msg;

    HeadlessErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
