package org.quyq.gwsu.common.ai.config;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.session.DatabaseStateStore;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import javax.sql.DataSource;

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
    public AgentStateStore databaseAgentSession(DataSource dataSource) {
        return new DatabaseStateStore(dataSource);
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


}
