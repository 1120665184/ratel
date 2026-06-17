package org.quyq.gwsu.security.headless.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.security.headless.enums.GraphRouteType;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouterInfo {

    /**
     * 路由类型
     */
    private GraphRouteType type;

    /**
     * 审批的信息
     */
    private ApprovalInfo approvalInfo;

    /**
     * 用户回答的信息
     * key: 模型提问的问题
     * value: 用户的回答
     */
    private Map<String , String> answerInfo;

    /**
     * 用户回答问题对应工具的toolCallId
     */
    private String toolCallId;

    /**
     * 判断为未知时，给用户的提问回复
     */
    private String unknownReply;




    public record ApprovalInfo(
            /**
             * 是否同意
             */
            boolean agree ,
            /**
             * 失败原因
             */
            String refuseReason
    ){}


}
