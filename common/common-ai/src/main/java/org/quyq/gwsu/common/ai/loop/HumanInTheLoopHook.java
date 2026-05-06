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
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
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

            List<ApprovalTips> neetHumanToolNames = toolCalls.stream()
                    .map(tool -> agent.getToolkit().getTool(tool.getName()))
                    .filter(Objects::nonNull)
                    .filter(t -> Objects.nonNull(t.needHumanInTheLoop()) && ApprovalStage.POST_REASONING == t.needHumanInTheLoop().stage())

                    .map(tool -> new ApprovalTips(tool.getName(), tool.needHumanInTheLoop().tip()))
                    .toList();

            if (CollUtil.isNotEmpty(neetHumanToolNames)) {
                msg.getMetadata().put(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY, neetHumanToolNames);
                e.stopAgent();
            }

        } else if (event instanceof PostActingEvent e) {
            ToolResultBlock toolResult = e.getToolResult();

            HumanInTheLoop humanInTheLoop = agent.getToolkit().getTool(toolResult.getName()).needHumanInTheLoop();
            if (Objects.nonNull(humanInTheLoop) && ApprovalStage.POST_ACTING == humanInTheLoop.stage()) {
                e.getToolResultMsg().getMetadata().put(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY,
                        Collections.singletonList(new ApprovalTips(toolResult.getName(), humanInTheLoop.tip()))
                );
                e.stopAgent();
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
