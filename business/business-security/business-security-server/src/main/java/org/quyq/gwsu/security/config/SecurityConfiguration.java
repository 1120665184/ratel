package org.quyq.gwsu.security.config;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.resolver.AguiAgentRegistry;
import org.quyq.gwsu.security.brain.service.IBrainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Quyq
 * @date 2026/4/23
 * @description
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final IBrainService brainService;

    @Bean
    public AguiAgentRegistry brainAgentRegistry(){
        AguiAgentRegistry registry = new AguiAgentRegistry();
        registry.registerFactory(IBrainService.AGENT_ID , brainService::getOrCreateSingletonAgent);
        return registry;
    }

}
