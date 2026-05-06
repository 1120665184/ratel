package org.quyq.gwsu.common.ai.loop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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

}
