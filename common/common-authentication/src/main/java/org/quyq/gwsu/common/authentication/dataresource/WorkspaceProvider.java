package org.quyq.gwsu.common.authentication.dataresource;


import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description 工作区列表提供者实现接口
 */
public interface WorkspaceProvider<U extends UserInfo> {

    /**
     * 获取该用户的工作区列表
     * @param user
     * @return
     */
    List<WorkspaceInfo> list(U user);

}
