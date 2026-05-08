package org.quyq.gwsu.common.security.domain;


import org.quyq.gwsu.common.security.enums.DataScope;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/8
 * @description 数据权限信息容器
 */
public record DataPermissionInfo(
        /**
         * 拥有的数据权限范围
         */
        DataScope dataScope ,
        /**
         * 数据权限值
         */
        Map<String, List<?>> permissions
) {
}
