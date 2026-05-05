package org.quyq.gwsu.common.authentication.dataresource;


import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description 数据资源管理器
 */
public class DataResourceScopeManager {

    private static List<DataResourceAttributeProvider> attributeProviders;

    private static WorkspaceProvider<UserInfo> workspaceProvider;

    public DataResourceScopeManager(@Nullable List<DataResourceAttributeProvider> attrProviders,
                                    @Nullable WorkspaceProvider<? extends UserInfo> workspace) {
        attributeProviders = Optional.ofNullable(attrProviders).orElse(Collections.emptyList());
        workspaceProvider = (WorkspaceProvider<UserInfo>) workspace;
    }


    /**
     * 获取指定工作区的数据资源
     *
     * @param workspaceInfo
     * @return
     */
    public static Map<String, List<?>> dataResource(WorkspaceInfo workspaceInfo, UserInfo userInfo, DataScope scope) {
        if (CollectionUtils.isEmpty(attributeProviders)) {
            return Collections.emptyMap();
        }

        Map<String, List<?>> tmp = new HashMap<>();
        //加载所有当前用户在指定工作区的数据资源数据。
        attributeProviders.forEach(provider ->
                tmp.put(provider.keyInfo().key(), provider.datas(workspaceInfo, userInfo, scope))
        );


        return tmp;
    }


    /**
     * 加载指定用户的工作区列表
     *
     * @param user
     * @param <T>
     * @return
     */
    public static <T extends UserInfo> @NonNull List<WorkspaceInfo> workspaceList(T user) {
        if (Objects.isNull(workspaceProvider)) {
            return Collections.singletonList(AuthenticationConstants.DataResource.DEFAULT_WORKSPACE);
        }

        return workspaceProvider.list(user);
    }


}
