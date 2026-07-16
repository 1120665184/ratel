package org.quyq.gwsu.common.ai.agui;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.processor.AgentResolver;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 始终返回同一个 Agent 实例的解析器。
 *
 * @author Quyq
 * @date 2026/7/12
 */
public class SingletonAgentResolver implements AgentResolver {

    private final Supplier<Agent> agentSupplier;

    public SingletonAgentResolver(Agent agent) {
        this(() -> Objects.requireNonNull(agent, "agent cannot be null"));
    }

    public static SingletonAgentResolver lazy(Supplier<Agent> agentSupplier) {
        return new SingletonAgentResolver(agentSupplier);
    }

    private SingletonAgentResolver(Supplier<Agent> agentSupplier) {
        this.agentSupplier = Objects.requireNonNull(agentSupplier, "agentSupplier cannot be null");
    }

    @Override
    public Agent resolveAgent(String agentId, String threadId) {
        return Objects.requireNonNull(agentSupplier.get(), "agent cannot be null");
    }

    @Override
    public boolean hasMemory(String threadId) {
        return false;
    }
}
