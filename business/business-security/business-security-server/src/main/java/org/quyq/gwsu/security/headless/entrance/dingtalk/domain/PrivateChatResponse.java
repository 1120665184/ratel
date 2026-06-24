package org.quyq.gwsu.security.headless.entrance.dingtalk.domain;

import java.util.List;

/**
 * @author Quyq
 * @date 2025/12/19
 * @description
 */
public record PrivateChatResponse(
        /**
         * 消息id
         */
        String processQueryKey,
        /**
         * 无效的用户userId列表。
         */
        List<String> invalidStaffIdList,
        /**
         * 被限流的userId列表
         */
        List<String> flowControlledStaffIdList
){
}
