package org.quyq.gwsu.common.ai.agui.dto;


import org.quyq.gwsu.common.ai.agui.model.RunAgentInput;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
public record ChatDTO(
        String method ,
        Map<String , Object> params ,
        RunAgentInput body
) {
}
