package org.quyq.gwsu.common.security.config;


import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.database.metadata.DdlFactory;
import org.quyq.gwsu.common.database.utils.DatabaseHelper;
import org.quyq.gwsu.common.security.dataresource.DataResourceInterceptor;
import org.quyq.gwsu.common.security.dataresource.DataResourceRuleUtils;
import org.quyq.gwsu.common.security.db.DefaultMetaObjectHandler;
import org.quyq.gwsu.common.security.filter.PropertiesSettingFilter;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.quyq.gwsu.common.security.service.impl.SQLExecutionServiceImpl;
import org.quyq.gwsu.common.security.utils.DataPermissionUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.request.RequestAttributes;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description
 */
@AutoConfiguration
@ConditionalOnClass({DynamicDataSourceContextHolder.class, JdbcTemplate.class})
public class SecurityDBConfiguration {

    @Bean
    public ISQLExecutionService sqlExecutionService(JdbcTemplate jdbcTemplate, DataPermissionUtils dataPermissionUtils,
                                                    DatabaseHelper databaseHelper, DdlFactory ddlFactory) {
        return new SQLExecutionServiceImpl(jdbcTemplate, dataPermissionUtils, databaseHelper, ddlFactory);
    }

    @Bean
    @ConditionalOnClass({DdlFactory.class})
    public DataPermissionUtils dataPermissionUtils(DataResourceRuleUtils ruleUtils, SecurityUtils securityUtils,
                                                   SessionUtils sessionUtils, DdlFactory ddlFactory) {
        return new DataPermissionUtils(ruleUtils, securityUtils, sessionUtils, ddlFactory);
    }

    @Bean
    @ConditionalOnClass(value = {RequestAttributes.class})
    public PropertiesSettingFilter propertiesSettingFilter(SecurityUtils securityUtils,
                                                           SessionUtils sessionUtils,
                                                           DataPermissionUtils dataPermissionUtils) {
        return new PropertiesSettingFilter(securityUtils, sessionUtils, dataPermissionUtils);
    }

    @Bean
    public DataResourceRuleUtils dataResourceRuleUtils(CacheUtils cacheUtils) {
        return new DataResourceRuleUtils(cacheUtils);
    }

    @Bean
    public DataResourceInterceptor dataResourceInterceptor(DataPermissionUtils dataPermissionUtils) {
        return new DataResourceInterceptor(dataPermissionUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass({MetaObjectHandler.class})
    public MetaObjectHandler defaultMetaObjectHandler(SecurityUtils securityUtils) {
        return new DefaultMetaObjectHandler(securityUtils);
    }

}
