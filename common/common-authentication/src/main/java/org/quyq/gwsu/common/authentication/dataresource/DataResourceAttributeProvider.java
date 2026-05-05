package org.quyq.gwsu.common.authentication.dataresource;


import org.quyq.gwsu.common.authentication.dataresource.domain.ResourceRuleKeyProperties;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.enums.DataScope;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description 数据资源属性提供者
 */
public interface DataResourceAttributeProvider {

    /**
     * 数据资源key信息
     *
     * @return
     */
    ResourceRuleKeyProperties keyInfo();

    /**
     * 数据资源
     *
     * @param workspace
     * @param userInfo
     * @param dataScope
     * @return
     */
    List<?> datas(WorkspaceInfo workspace, UserInfo userInfo, DataScope dataScope);

}
