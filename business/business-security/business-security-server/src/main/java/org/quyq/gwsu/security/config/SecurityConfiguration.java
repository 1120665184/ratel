package org.quyq.gwsu.security.config;


import io.agentscope.core.session.Session;
import org.quyq.gwsu.common.ai.session.DatabaseSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author Quyq
 * @date 2026/4/23
 * @description
 */
@Configuration
public class SecurityConfiguration {

    /**
     * 智能体会话记录表名
     */
    public static final String SESSION_TABLE_NAME = "security_brain_sessions";


    /**
     * 智能体上下文持久化
     *
     * @param dataSource
     * @return
     */
    @Bean
    public Session databaseAgentSession(DataSource dataSource) {
        return new DatabaseSession(dataSource, SESSION_TABLE_NAME);
    }

}
