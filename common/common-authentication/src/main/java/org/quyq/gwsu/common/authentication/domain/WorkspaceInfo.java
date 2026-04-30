package org.quyq.gwsu.common.authentication.domain;


/**
 * @author Quyq
 * @date 2026/4/13
 * @description 工作空间信息
 */
public record WorkspaceInfo(
        /**
         * 工作空间唯一标识
         */
        String id ,
        /**
         * 工作空间名称
         */
        String name
) {
}
