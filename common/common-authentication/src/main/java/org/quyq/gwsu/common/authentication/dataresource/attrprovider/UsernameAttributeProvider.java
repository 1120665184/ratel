package org.quyq.gwsu.common.authentication.dataresource.attrprovider;


import org.quyq.gwsu.common.authentication.dataresource.DataResourceAttributeProvider;
import org.quyq.gwsu.common.authentication.dataresource.domain.ResourceRuleKeyProperties;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.enums.DataScope;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/14
 * @description 加载用户名数据资源
 */
public class UsernameAttributeProvider implements DataResourceAttributeProvider {
    @Override
    public ResourceRuleKeyProperties keyInfo() {
        return new ResourceRuleKeyProperties("username", "用户名");
    }

    @Override
    public List<?> datas(WorkspaceInfo workspace, UserInfo userInfo, DataScope dataScope) {
        return List.of(userInfo.getUserName());
    }
}
