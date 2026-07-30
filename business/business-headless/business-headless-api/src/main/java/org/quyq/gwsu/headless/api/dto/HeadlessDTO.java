package org.quyq.gwsu.headless.api.dto;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/6/25
 * @description 无头智能体流式请求
 */
public record HeadlessDTO(
        String text,
        List<HeadlessResourceDTO> resources,
        String threadId
) {

    public boolean hasContent() {
        return text != null && !text.isBlank()
                || resources != null && !resources.isEmpty();
    }
}
