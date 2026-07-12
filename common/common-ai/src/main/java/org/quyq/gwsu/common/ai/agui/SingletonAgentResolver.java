package org.quyq.gwsu.common.ai.agui;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.processor.AgentResolver;

import java.util.Objects;

/**
 * 始终返回同一个 Agent 实例的解析器。
 *
 * @author Quyq
 * @date 2026/7/12
 */
public class SingletonAgentResolver implements AgentResolver {

    private final Agent agent;

    public SingletonAgentResolver(Agent agent) {
        this.agent = Objects.requireNonNull(agent, "agent cannot be null");
    }

    @Override
    public Agent resolveAgent(String agentId, String threadId) {
        return agent;
    }

    @Override
    public boolean hasMemory(String threadId) {
        return false;
    }
}
