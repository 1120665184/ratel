package org.quyq.gwsu.common.ai.loop;


/**
 * @author Quyq
 * @date 2026/5/6
 * @description 人工审批阶段
 */
public enum ApprovalStage {

    /**
     * 推理后
     * 模型决定要调用哪些工具后，在实际执行前暂停。此时你可以看到工具名称和参数，让用户决定是否允许执行
     */
    POST_REASONING ,
    /**
     * 行动后
     * 工具执行完毕后，在进入下一轮推理前暂停。此时你可以看到执行结果，让用户决定是否继续
     */
    POST_ACTING ;

}
