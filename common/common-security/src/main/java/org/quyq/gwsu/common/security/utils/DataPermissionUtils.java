package org.quyq.gwsu.common.security.utils;


import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.DataPermissionInfo;

import java.util.*;

/**
 * @author Quyq
 * @date 2026/5/8
 * @description 数据权限信息存储工具类
 */
@RequiredArgsConstructor
public class DataPermissionUtils {

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final ThreadLocal<DataPermissionInfo> dataPermissionInfos = new TransmittableThreadLocal<>();


    /**
     * 获取当前用户的数据权限信息
     * @return
     */
    public DataPermissionInfo getUserDataPermission() {
        DataPermissionInfo info = dataPermissionInfos.get();
        if (Objects.nonNull(info)) {
            return info;
        }

        return securityUtils.getSubject()
                .map(subject -> {

                    Optional<Map<String, List<?>>> dataResource = sessionUtils.getValue(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE);
                    return new DataPermissionInfo(subject.getDataScope(), dataResource.orElse(Collections.emptyMap()));
                }).orElse(null);

    }

    /**
     * 设置当前用户的数据权限缓存
     *
     * @param info
     */
    public void setDataPermission(DataPermissionInfo info) {
        dataPermissionInfos.set(info);
    }

    /**
     * 获取当前用户名
     * @return 当前用户名，未登录时返回 null
     */
    public String getCurrentUsername() {
        return securityUtils.getUsername();
    }

    /**
     * 清空当前用户的数据权限缓存
     */
    public void clearDataPermission() {
        dataPermissionInfos.remove();
    }
}
