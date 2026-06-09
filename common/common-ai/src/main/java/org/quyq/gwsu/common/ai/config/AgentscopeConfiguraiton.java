package org.quyq.gwsu.common.ai.config;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.loop.HumanInTheLoopHook;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
@AutoConfiguration
@ConditionalOnClass(ReActAgent.class)
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
    public WebToolUtils webToolUtils(CacheUtils cacheUtils) {
        //人工审批注解判断
        AgentBase.addSystemHook(new HumanInTheLoopHook());
        return new WebToolUtils(cacheUtils);
    }


}
