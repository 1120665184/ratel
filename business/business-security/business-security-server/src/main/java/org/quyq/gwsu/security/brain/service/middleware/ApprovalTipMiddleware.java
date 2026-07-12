package org.quyq.gwsu.security.brain.service.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventEmitter;
import io.agentscope.core.event.CustomEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.permission.PermissionEngine;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.AgentApprovalResolver;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import reactor.core.publisher.Flux;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;

/**
 * 在 AgentScope v2 权限拦截进入 ASK 时，回填审批提示到最终 assistant 消息 metadata。
 */
@RequiredArgsConstructor
public class ApprovalTipMiddleware implements MiddlewareBase {

    private final ObjectMapper objectMapper;

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext ctx,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        List<ApprovalTips> approvalTips = collectApprovalTips(agent, ctx, input);
        if (approvalTips.isEmpty()) {
            return next.apply(input);
        }

        return Flux.deferContextual(contextView -> {
            AgentEventEmitter emitter = AgentEventEmitter.fromContext(contextView).orElse(null);
            return next.apply(input)
                    .map(event -> attachApprovalTips(event, ctx, approvalTips, emitter));
        });
    }

    private List<ApprovalTips> collectApprovalTips(Agent agent, RuntimeContext ctx, ActingInput input) {
        if (agent == null || input == null || input.toolCalls() == null || input.toolCalls().isEmpty()) {
            return List.of();
        }

        Toolkit toolkit = agent.getToolkit();
        AgentState agentState = RuntimeContext.resolveAgentState(ctx, agent);
        PermissionContextState permissionContext = agentState != null
                ? agentState.getPermissionContext()
                : PermissionContextState.builder().build();
        PermissionEngine permissionEngine = new PermissionEngine(permissionContext);

        List<ApprovalTips> tips = new ArrayList<>();
        for (ToolUseBlock toolCall : input.toolCalls()) {
            ApprovalTips tip = evaluateApprovalTip(toolkit, permissionEngine, toolCall);
            if (tip != null) {
                tips.add(tip);
            }
        }
        return tips;
    }

    private ApprovalTips evaluateApprovalTip(
            Toolkit toolkit,
            PermissionEngine permissionEngine,
            ToolUseBlock toolCall) {
        if (toolkit == null || toolCall == null) {
            return null;
        }

        AgentTool agentTool = toolkit.getTool(toolCall.getName());
        if (!(agentTool instanceof ToolBase toolBase)) {
            return null;
        }

        PermissionDecision decision = resolveApprovalDecision(toolBase, permissionEngine, toolCall);
        if (decision == null || decision.getBehavior() != PermissionBehavior.ASK) {
            return null;
        }

        return new ApprovalTips(
                toolCall.getId(),
                toolCall.getName(),
                decision.getMessage(),
                ApprovalStage.POST_REASONING);
    }

    private PermissionDecision resolveApprovalDecision(
            ToolBase toolBase,
            PermissionEngine permissionEngine,
            ToolUseBlock toolCall) {
        Map<String, Object> input = toolCall.getInput();
        PermissionContextState context = permissionEngine.getContext();

        PermissionDecision toolDecision = toolBase.checkPermissions(input, context).block();
        if (toolDecision != null && toolDecision.getBehavior() == PermissionBehavior.ASK) {
            return toolDecision;
        }

        return permissionEngine.checkPermission(toolBase, input).block();
    }

    AgentEvent attachApprovalTips(AgentEvent event, RuntimeContext ctx, List<ApprovalTips> approvalTips) {
        return attachApprovalTips(event, ctx, approvalTips, null);
    }

    AgentEvent attachApprovalTips(
            AgentEvent event,
            RuntimeContext ctx,
            List<ApprovalTips> approvalTips,
            AgentEventEmitter emitter) {
        if (!(event instanceof RequireUserConfirmEvent requireUserConfirmEvent)
                || CollectionUtils.isEmpty(approvalTips)) {
            return event;
        }

        RequireUserConfirmEvent enrichedEvent = enrichRequireUserConfirmEvent(requireUserConfirmEvent, approvalTips);
        syncLastAssistantMsg(ctx, enrichedEvent);
        emitHumanApprovalEvent(enrichedEvent, emitter);
        return enrichedEvent;
    }

    RequireUserConfirmEvent enrichRequireUserConfirmEvent(
            RequireUserConfirmEvent event, List<ApprovalTips> approvalTips) {
        if (event == null || CollectionUtils.isEmpty(event.getToolCalls()) || CollectionUtils.isEmpty(approvalTips)) {
            return event;
        }

        Map<String, ApprovalTips> tipsByCallId = new LinkedHashMap<>();
        approvalTips.forEach(tip -> tipsByCallId.putIfAbsent(tip.callId(), tip));

        List<ToolUseBlock> enrichedToolCalls = event.getToolCalls().stream()
                .map(toolCall -> attachApprovalTip(toolCall, tipsByCallId.get(toolCall.getId())))
                .toList();
        RequireUserConfirmEvent enrichedEvent = new RequireUserConfirmEvent(
                event.getId(),
                event.getCreatedAt(),
                event.getReplyId(),
                enrichedToolCalls);
        enrichedEvent.withSource(event.getSource());
        enrichedEvent.withMetadata(event.getMetadata());
        return enrichedEvent;
    }

    private ToolUseBlock attachApprovalTip(ToolUseBlock toolCall, ApprovalTips approvalTips) {
        if (toolCall == null || approvalTips == null) {
            return toolCall;
        }

        Map<String, Object> metadata = new LinkedHashMap<>(toolCall.getMetadata());
        metadata.put(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY, objectMapper.writeValueAsString(approvalTips));
        return new ToolUseBlock(
                toolCall.getId(),
                toolCall.getName(),
                toolCall.getInput(),
                toolCall.getContent(),
                metadata,
                toolCall.getState());
    }

    private void syncLastAssistantMsg(RuntimeContext ctx, RequireUserConfirmEvent event) {
        if (ctx == null || event == null || CollectionUtils.isEmpty(event.getToolCalls())) {
            return;
        }

        AgentState agentState = RuntimeContext.resolveAgentState(ctx, null);
        if (agentState == null || agentState.contextMutable().isEmpty()) {
            return;
        }

        List<Msg> messages = agentState.contextMutable();
        Map<String, ToolUseBlock> toolCallsById = new LinkedHashMap<>();
        event.getToolCalls().forEach(toolCall -> toolCallsById.put(toolCall.getId(), toolCall));
        for (int i = messages.size() - 1; i >= 0; i--) {
            Msg msg = messages.get(i);
            if (!isApprovalTargetAssistantMsg(msg, toolCallsById.keySet())) {
                continue;
            }

            List<ContentBlock> updatedContent = msg.getContent().stream()
                    .map(contentBlock -> {
                        if (contentBlock instanceof ToolUseBlock toolUseBlock) {
                            ToolUseBlock enrichedToolCall = toolCallsById.get(toolUseBlock.getId());
                            if (enrichedToolCall == null) {
                                return toolUseBlock;
                            }
                            return mergeToolCallMetadata(toolUseBlock, enrichedToolCall);
                        }
                        return contentBlock;
                    })
                    .toList();
            messages.set(i, msg.withContent(updatedContent));
            return;
        }
    }

    private ToolUseBlock mergeToolCallMetadata(ToolUseBlock originalToolCall, ToolUseBlock enrichedToolCall) {
        Map<String, Object> metadata = new LinkedHashMap<>(originalToolCall.getMetadata());
        metadata.putAll(enrichedToolCall.getMetadata());
        return new ToolUseBlock(
                originalToolCall.getId(),
                originalToolCall.getName(),
                originalToolCall.getInput(),
                originalToolCall.getContent(),
                metadata,
                originalToolCall.getState());
    }

    private void emitHumanApprovalEvent(RequireUserConfirmEvent event, AgentEventEmitter emitter) {
        if (event == null || emitter == null) {
            return;
        }

        HumanApprovalInfo approvalInfo = AgentApprovalResolver.buildReasoningApprovalInfo(event.getToolCalls());
        if (approvalInfo == null) {
            return;
        }

        emitter.emit(new CustomEvent(
                AIConstants.AguiCustomEvent.HUMAN_APPROVAL,
                objectMapper.convertValue(approvalInfo, Map.class)));
    }

    private boolean isApprovalTargetAssistantMsg(Msg msg, Set<String> targetToolCallIds) {
        if (msg == null || msg.getRole() != MsgRole.ASSISTANT || CollectionUtils.isEmpty(targetToolCallIds)) {
            return false;
        }

        return msg.getContentBlocks(ToolUseBlock.class).stream()
                .map(ToolUseBlock::getId)
                .anyMatch(targetToolCallIds::contains);
    }
}
