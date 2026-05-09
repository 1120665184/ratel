package org.quyq.gwsu.common.ai.loop;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 工具审批条件判断接口
 * <p>
 * 实现 invoke 方法的类可作为 {@link HumanInTheLoop#reasoningCondition()} 的值，
 * 用于动态决定工具调用是否需要人工审批。
 * <p>
 * 实现类需为无参构造的类，运行时通过反射实例化并调用。
 *
 * @see HumanInTheLoop#reasoningCondition() ()
 */
@FunctionalInterface
public interface ApprovalCondition {

    /**
     * 根据工具调用参数判断是否需要人工审批
     *
     * @param args 当前工具调用的输入参数
     * @return true 表示需要人工审批，false 表示跳过审批直接执行
     */
    Outcome invoke(Map<String, Object> args);


    record Outcome(
            /**
             * 是否需要审批
             */
            boolean needApproval,
            /**
             * 审批提示内容
             */
            @Nullable
            String tip
    ) {
    }

}
