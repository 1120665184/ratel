package org.quyq.gwsu.common.ai.loop.domain;

/**
 * 人工审批结果
 *
 * @param result       审批结果：APPROVED（同意）或 REJECTED（拒绝）
 * @param rejectReason 拒绝原因，可选，仅在 POST_REASONING 阶段填写
 */
public record ApprovalResult(
        ApprovalEnum result,
        String rejectReason
) {

    /**
     * 判断是否为同意
     */
    public boolean isApproved() {
        return ApprovalEnum.APPROVED.equals(result);
    }


    public enum ApprovalEnum {
        /**
         * 同意
         */
        APPROVED ,
        /**
         * 拒绝
         */
        REJECTED;
    }

}
