package org.quyq.gwsu.headless.api.dto;

/**
 * @author Quyq
 * @date 2026/7/28
 * @description 无头智能体资源入参。调用方仅需提供 fileId，服务端会补齐其余信息。
 */
public record HeadlessResourceDTO(
        String fileId,
        String url,
        String mimeType,
        String fileName
) {

    public HeadlessResourceDTO(String fileId) {
        this(fileId, null, null, null);
    }
}
