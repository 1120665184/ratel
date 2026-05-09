package org.quyq.gwsu.common.ai.loop;


import cn.hutool.core.collection.CollUtil;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.StructuredOutputCapableAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.tool.AgentTool;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description 人工审批注解判断钩子
 */
public class HumanInTheLoopHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        StructuredOutputCapableAgent agent = getAgent(event);
        if (Objects.isNull(agent)) {
            return Mono.just(event);
        }


        if (event instanceof PostReasoningEvent e) {
            Msg msg = e.getReasoningMessage();
            List<ToolUseBlock> toolCalls = msg.getContentBlocks(ToolUseBlock.class);

            List<ApprovalTips> needHumanToolNames = toolCalls.stream()
                    .map(tool -> {
                        AgentTool agentTool = agent.getToolkit().getTool(tool.getName());
                        if (Objects.isNull(agentTool)) return null;
                        // 传入工具调用参数，动态判断是否需要审批
                        ApprovalTips tips = agentTool.needHumanInTheLoop(tool.getInput());
                        if (Objects.isNull(tips) || ApprovalStage.POST_REASONING != tips.stage()) {
                            return null;
                        }
                        return tips;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (CollUtil.isNotEmpty(needHumanToolNames)) {
                msg.getMetadata().put(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY, needHumanToolNames);
                e.stopAgent();
            }

        } else if (event instanceof PostActingEvent e) {
            Msg msg = e.getToolResultMsg();
            ToolResultBlock toolResult = e.getToolResult();

            AgentTool agentTool = agent.getToolkit().getTool(toolResult.getName());
            if (Objects.nonNull(agentTool)) {
                // POST_ACTING 阶段：使用工具执行时的输入参数
                Map<String, Object> toolInput = e.getToolUse() != null
                        ? e.getToolUse().getInput()
                        : Map.of();
                ApprovalTips tips = agentTool.needHumanInTheLoop(toolInput);
                if (Objects.nonNull(tips) && ApprovalStage.POST_ACTING == tips.stage()) {
                    msg.getMetadata().put(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY,
                            Collections.singletonList(tips)
                    );

                    e.stopAgent();
                }
            }
        }


        return Mono.just(event);
    }


    public StructuredOutputCapableAgent getAgent(HookEvent event) {
        Agent agent = event.getAgent();
        if (agent instanceof StructuredOutputCapableAgent tmp) {
            return tmp;
        }
        return null;
    }


}
