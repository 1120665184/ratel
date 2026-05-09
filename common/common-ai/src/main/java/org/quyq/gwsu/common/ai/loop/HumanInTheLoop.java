package org.quyq.gwsu.common.ai.loop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description 智能体调用工具需要人工审批时的注解 ， 在工具上使用
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface HumanInTheLoop {

    /**
     * 告警提示信息
     * @return
     */
    String tip();

    /**
     * 人工审批阶段
     * @return
     */
    ApprovalStage stage() default ApprovalStage.POST_REASONING;

    /**
     * 推理阶段（POST_REASONING）审批条件判断类，用于动态决定是否需要人工审批。
     * <p>
     * 仅在 {@link ApprovalStage#POST_REASONING} 阶段生效，
     * {@link ApprovalStage#POST_ACTING} 阶段始终需要审批（无需条件判断）。
     * <p>
     * 未配置时（默认 {@link AlwaysApprovalCondition}），表示始终需要审批。
     * 配置后，运行时实例化该类并调用 {@link ApprovalCondition#invoke(java.util.Map)}，
     * 传入当前工具调用参数，返回 true 表示需要审批。
     *
     * @return 审批条件判断实现类
     * @see ApprovalCondition
     */
    Class<? extends ApprovalCondition> reasoningCondition() default AlwaysApprovalCondition.class;



    class AlwaysApprovalCondition implements ApprovalCondition {

        @Override
        public Outcome invoke(Map<String, Object> args) {
            return new Outcome(true , null);
        }
    }

}
