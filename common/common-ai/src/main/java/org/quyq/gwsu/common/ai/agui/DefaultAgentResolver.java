/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quyq.gwsu.common.ai.agui;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.AguiException;
import io.agentscope.core.agui.processor.AgentResolver;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import io.agentscope.core.session.Session;
import org.quyq.gwsu.common.ai.agui.web.WebToolExecuteHook;
import org.quyq.gwsu.common.ai.session.CommonSessionKey;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Default implementation of {@link AgentResolver} for Spring Boot integration.
 *
 * <p>This resolver supports two modes:
 * <ul>
 *   <li><b>Simple mode</b>: Directly looks up agents from the registry</li>
 *   <li><b>Session mode</b>: Uses {@link ThreadSessionManager} for server-side memory</li>
 * </ul>
 */
public class DefaultAgentResolver implements AgentResolver {

    private final AguiAgentRegistry registry;
    private final ThreadSessionManager sessionManager;
    private final boolean serverSideMemory;

    /**
     * 持久化session实现
     */
    private final Session session;

    /**
     * 获取当前用户函数
     */
    private final Supplier<String> getUserId;

    /**
     * Creates a simple resolver without session support.
     *
     * @param registry The agent registry
     */
    public DefaultAgentResolver(AguiAgentRegistry registry) {
        this(registry, null, false, null, null);
    }

    /**
     * Creates a resolver with optional session support.
     *
     * @param registry         The agent registry
     * @param sessionManager   The session manager (may be null)
     * @param serverSideMemory Whether to enable server-side memory
     */
    public DefaultAgentResolver(
            AguiAgentRegistry registry,
            ThreadSessionManager sessionManager,
            boolean serverSideMemory, Session session, Supplier<String> getUserId) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.sessionManager = sessionManager;
        this.serverSideMemory = serverSideMemory && sessionManager != null;
        this.session = session;
        this.getUserId = getUserId;
    }

    @Override
    public Agent resolveAgent(String agentId, String threadId) {
        if (serverSideMemory && sessionManager != null) {
            // Server-side memory mode: use session manager
            return sessionManager.getOrCreateAgent(
                    threadId,
                    agentId,
                    () -> createAgent(agentId, threadId));
        } else {
            // Standard mode: create new agent for each request
            return createAgent(agentId, threadId);
        }
    }

    private Agent createAgent(String agentId, String threadId) {
        Agent agent = registry.getAgent(agentId)
                .orElseThrow(() -> new AguiException.AgentNotFoundException(agentId));
        if (Objects.nonNull(session) && agent instanceof ReActAgent raa) {
            raa.getHooks().add(new WebToolExecuteHook(threadId));
            String userId = null;
            if (Objects.nonNull(getUserId)) {
                userId = getUserId.get();
            }
            raa.loadIfExists(session, CommonSessionKey.of(threadId, userId));
        }
        return agent;

    }


    @Override
    public boolean hasMemory(String threadId) {
        if (serverSideMemory && sessionManager != null) {
            return sessionManager.hasMemory(threadId);
        }
        return false;
    }

    /**
     * Creates a new builder for DefaultAgentResolver.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DefaultAgentResolver.
     */
    public static class Builder {

        private AguiAgentRegistry registry;
        private ThreadSessionManager sessionManager;
        private boolean serverSideMemory = false;
        private Session session;
        private Supplier<String> getUserId;

        /**
         * Set the agent registry.
         *
         * @param registry The agent registry
         * @return This builder
         */
        public Builder registry(AguiAgentRegistry registry) {
            this.registry = registry;
            return this;
        }

        /**
         * Set the session manager for server-side memory support.
         *
         * @param sessionManager The session manager
         * @return This builder
         */
        public Builder sessionManager(ThreadSessionManager sessionManager) {
            this.sessionManager = sessionManager;
            return this;
        }

        public Builder session(Session session) {
            this.session = session;
            return this;
        }

        public Builder getUserIdSupplier(Supplier<String> supplier) {
            getUserId = supplier;
            return this;
        }

        /**
         * Enable or disable server-side memory management.
         *
         * @param enabled Whether to enable server-side memory
         * @return This builder
         */
        public Builder serverSideMemory(boolean enabled) {
            this.serverSideMemory = enabled;
            return this;
        }

        /**
         * Build the resolver.
         *
         * @return The built resolver
         * @throws NullPointerException if registry is not set
         */
        public DefaultAgentResolver build() {
            return new DefaultAgentResolver(registry, sessionManager, serverSideMemory, session, getUserId);
        }
    }
}
