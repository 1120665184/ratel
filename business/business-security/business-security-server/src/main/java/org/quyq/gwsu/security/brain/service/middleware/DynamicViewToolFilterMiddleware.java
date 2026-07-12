package org.quyq.gwsu.security.brain.service.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ToolSchema;
import org.quyq.gwsu.common.ai.agui.domain.AIRunnerInstanceWrapper;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 按当前请求的访问形态动态过滤界面操作工具，避免单例 Agent 在不同登录形态之间串扰。
 */
public class DynamicViewToolFilterMiddleware implements MiddlewareBase {

    private static final Set<String> HEADLESS_DISABLED_TOOLS = Set.of("EnterAiMode", "ExitAiMode");

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext ctx,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {
        if (!isHeadless(ctx) || input == null || input.tools() == null || input.tools().isEmpty()) {
            return next.apply(input);
        }

        List<ToolSchema> filteredTools = input.tools().stream()
                .filter(tool -> !HEADLESS_DISABLED_TOOLS.contains(tool.getName()))
                .toList();
        return next.apply(new ReasoningInput(input.messages(), filteredTools, input.options()));
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext ctx,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        if (!isHeadless(ctx) || input == null || input.toolCalls() == null || input.toolCalls().isEmpty()) {
            return next.apply(input);
        }

        List<ToolUseBlock> filteredToolCalls = input.toolCalls().stream()
                .filter(toolCall -> !HEADLESS_DISABLED_TOOLS.contains(toolCall.getName()))
                .toList();
        return next.apply(new ActingInput(filteredToolCalls));
    }

    private boolean isHeadless(RuntimeContext ctx) {
        AIRunnerInstanceWrapper wrapper = ctx != null ? ctx.get(AIRunnerInstanceWrapper.class) : null;
        return wrapper != null && wrapper.headless();
    }
}
