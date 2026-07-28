package org.quyq.gwsu.headless.api.dto;

/**
 * @author Quyq
 * @date 2026/7/28
 * @description 无头智能体资源入参，仅支持 URL 资源
 */
public record HeadlessResourceDTO(
        String url,
        String mimeType
) {
}
