package org.quyq.gwsu.common.ai.config;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.harness.agent.DistributedStore;
import org.quyq.gwsu.common.ai.agui.adapter.AguiAdapterConfig;
import org.quyq.gwsu.common.ai.agui.processor.AguiRequestProcessor;
import org.quyq.gwsu.common.ai.agui.resolver.AguiAgentRegistry;
import org.quyq.gwsu.common.ai.agui.resolver.MultiAgentResolver;
import org.quyq.gwsu.common.ai.agui.resolver.SingletonAgentResolver;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.distributed.redis.CacheRedisAgentStateStore;
import org.quyq.gwsu.common.ai.distributed.redis.CacheRedisDistributedStore;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
@AutoConfiguration
@ConditionalOnClass(ReActAgent.class)
public class AgentscopeConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public AgentStateStore agentStateStore(CacheUtils cacheUtils) {
        return new CacheRedisAgentStateStore(cacheUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public AguiRequestProcessor aguiRequestProcessor(ObjectProvider<List<AguiAgentRegistry>> agentRegistries) {
        AguiAgentRegistry registry = new AguiAgentRegistry();
        List<AguiAgentRegistry> ifAvailable = agentRegistries.getIfAvailable();
        if(!CollectionUtils.isEmpty(ifAvailable)) {
            ifAvailable.forEach(v ->v.getAllAgentFactories().forEach(registry::registerFactory));
        }

        return AguiRequestProcessor.builder()
                .agentResolver(new MultiAgentResolver(registry))
                .config(AguiAdapterConfig.builder()
                        .enableReasoning(true)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Toolkit agentscopeToolkit() {
        return new Toolkit(ToolkitConfig.builder()
                .parallel(true)
                .build());
    }


    @Bean
    public WebToolUtils webToolUtils(CacheUtils cacheUtils) {
        return new WebToolUtils(cacheUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedStore distributedStore(AgentStateStore agentStateStore, CacheUtils cacheUtils) {
        return new CacheRedisDistributedStore(agentStateStore, cacheUtils);
    }


}
