package org.quyq.gwsu.common.ai.agui.processor;

import io.agentscope.core.agent.Agent;

public interface AgentResolver {

    Agent resolveAgent(String agentId, String threadId);

    boolean hasMemory(String threadId);
}
