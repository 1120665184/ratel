package org.quyq.gwsu.kit.job.errcode;

import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

/**
 * 定时任务模块错误码
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.KIT_ERROR_CODE_MODULE, notes = "定时任务模块错误码")
public enum JobErrorCode implements ReturnCode {

    E02001("任务不存在"),
    E02002("执行器不存在"),
    E02003("任务处理器未找到"),
    E02004("Cron表达式无效"),
    E02005("任务已运行中"),
    E02006("执行器地址为空"),
    E02007("任务参数无效"),
    E02008("调度类型无效"),
    E02009("调度过期策略无效"),
    E02010("阻塞处理策略无效"),
    E02011("路由策略无效"),
    E02012("Glue类型无效"),
    E02013("请输入任务名称"),
    E02014("请输入负责人"),
    E02015("请选择执行器"),
    E02016("该执行器下存在任务，无法删除"),
    E02017("AppName已存在"),
    E02018("手动录入地址不能为空"),
    E02019("子任务ID({0})不存在"),
    E02020("子任务ID({0})无效"),
    E02021("调度类型为空，不允许启动"),
    E02022("请输入AppName"),
    E02023("请输入执行器名称"),
    E02024("日志ID无效"),
    E02025("日志清理类型无效"),
    E02026("至少保留一个执行器"),
    ;

    private final String msg;

    JobErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }

}
