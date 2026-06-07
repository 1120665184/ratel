package org.quyq.gwsu.kit.errcode;


import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

/**
 * @author Quyq
 * @date 2026/6/5
 * @description
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.KIT_ERROR_CODE_MODULE, notes = "工具套件模块错误码")
public enum KitErrorCode implements ReturnCode {

    // 文件上传模块错误码
    E01001("该功能加班加点实现中..."),
    E01002("文件上传失败"),
    E01003("文件下载失败"),
    E01004("未找到分片数据，合并失败"),
    E01005("文件合并失败"),
    E01006("文件分片上传失败"),
    E01007("文件删除失败"),
    E01008("附件上传缺少配置，请联系管理员"),
    E01009("解析文件媒体类型失败"),
    E01010("禁止上传空文件"),
    E01011("文件后缀被串改"),
    E01012("该类型文件被禁止上传"),
    E01013("文件类型校验失败"),
    E01014("未识别出文件类型"),

    ;

    private final String msg;

    KitErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
