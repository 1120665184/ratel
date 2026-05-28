package org.quyq.gwsu.common.ai.config;


import com.alibaba.ttl.threadpool.TtlExecutors;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.config.properties.AgentProperties;
import org.quyq.gwsu.common.ai.config.properties.AgentscopeProperties;
import org.quyq.gwsu.common.ai.loop.HumanInTheLoopHook;
import org.quyq.gwsu.common.ai.model.ModelProviderType;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.Executors;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentscopeProperties.class)
@ConditionalOnClass(ReActAgent.class)
@ConditionalOnProperty(prefix = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".agent", name = "enabled", havingValue = "true")
public class AgentscopeConfiguraiton {

    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Memory agentscopeMemory() {
        return new InMemoryMemory();
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
    @ConditionalOnMissingBean(Model.class)
    public Model agentscopeModel(AgentscopeProperties properties) {
        return ModelProviderType.fromProperties(properties).createModel(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ReActAgent agentscopeReActAgent(
            Model model, Memory memory, Toolkit toolkit, AgentscopeProperties properties) {
        AgentProperties config = properties.getAgent();
        return ReActAgent.builder()
                .name(config.getName())
                .sysPrompt(config.getSysPrompt())
                .model(model)
                .memory(memory)
                .toolkit(toolkit)
                .maxIters(config.getMaxIters())
                .build();
    }

    @Bean
    public WebToolUtils webToolUtils(CacheUtils cacheUtils) {
        AgentBase.addSystemHook(new HumanInTheLoopHook());
        return new WebToolUtils(cacheUtils);
    }


}
