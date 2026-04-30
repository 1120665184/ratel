package org.quyq.gwsu.common.authentication.config;


import org.quyq.gwsu.common.authentication.dataresource.DataResourceAttributeProvider;
import org.quyq.gwsu.common.authentication.dataresource.DataResourceScopeManager;
import org.quyq.gwsu.common.authentication.dataresource.WorkspaceProvider;
import org.quyq.gwsu.common.authentication.dataresource.attrprovider.UserIdAttributeProvider;
import org.quyq.gwsu.common.authentication.dataresource.attrprovider.UsernameAttributeProvider;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
@AutoConfiguration
public class DataResourceConfiguration {


    @Bean
    public DataResourceScopeManager dataResourceScopeManager(List<DataResourceAttributeProvider> providers,
                                                             ObjectProvider<WorkspaceProvider<? extends UserInfo>> workspaceProviders) {
        return new DataResourceScopeManager(providers, workspaceProviders.getIfAvailable());
    }


    @Bean
    public UserIdAttributeProvider userIdAttributeProvider() {
        return new UserIdAttributeProvider();
    }

    @Bean
    public UsernameAttributeProvider usernameAttributeProvider() {
        return new UsernameAttributeProvider();
    }

}
