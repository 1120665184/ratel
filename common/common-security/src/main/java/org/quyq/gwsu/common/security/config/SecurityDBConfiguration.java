package org.quyq.gwsu.common.security.config;


import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.quyq.gwsu.common.security.service.impl.SQLExecutionServiceImpl;
import org.quyq.gwsu.common.security.utils.SqlDataPermissionFilterUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description
 */
@AutoConfiguration
@ConditionalOnClass({DynamicDataSourceContextHolder.class, JdbcTemplate.class})
public class SecurityDBConfiguration {

    @Bean
    public ISQLExecutionService sqlExecutionService(JdbcTemplate jdbcTemplate, SqlDataPermissionFilterUtils sqlDataPermissionFilterUtils) {
        return new SQLExecutionServiceImpl(jdbcTemplate, sqlDataPermissionFilterUtils);
    }
}
