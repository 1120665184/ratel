package org.quyq.gwsu.security.brain.service.tool;

import org.quyq.gwsu.common.ai.loop.ApprovalCondition;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ClickElement 工具的审批条件判断
 * 当 tags 参数包含 "approval" 标签时，需要人工审批
 *
 * @see ApprovalCondition
 */
public class NeedClickApprovalCondition implements ApprovalCondition {

    private static final String APPROVAL_TAG = "approval";

    @Override
    public Outcome invoke(Map<String, Object> args) {
        Object tags = args.get("tags");
        String operationDescription = (String) args.get("operationDescription");
        String tipStart = "敏感操作审批：";
        String tip = null;
        boolean needApproval = false;
        if(Objects.nonNull(tags)) {
            if (StringUtils.hasText(operationDescription)) {
                tip = tipStart + operationDescription;
            }

            if (tags instanceof String tagsStr && StringUtils.hasText(tagsStr)) {
                needApproval = tagsStr.contains(APPROVAL_TAG);
            }
            // tags 为 List 类型时（某些LLM可能传数组）
            else if (tags instanceof List<?> tagsList) {
                needApproval = tagsList.stream().anyMatch(t -> APPROVAL_TAG.equals(String.valueOf(t)));
            }
        }


        return new Outcome(needApproval, tip);
    }
}
