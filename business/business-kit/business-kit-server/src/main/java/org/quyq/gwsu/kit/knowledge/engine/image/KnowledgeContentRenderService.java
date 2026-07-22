package org.quyq.gwsu.kit.knowledge.engine.image;

import org.quyq.gwsu.common.security.config.properties.universal.BaseProjectInfoProperties;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 知识内容渲染服务。
 */
@Service
public class KnowledgeContentRenderService {

    public String render(String content) {
        return KnowledgeImageMarkerSupport.renderToMarkdown(content, this::buildImageUrl);
    }

    private String buildImageUrl(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            return "";
        }
        BaseProjectInfoProperties properties = ConfigInfoUtils.getByObject(
                BaseProjectInfoProperties.CONFIG_KEY,
                BaseProjectInfoProperties.class);
        String baseUrl = properties.apiBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return "/kit/file/stream/%s".formatted(fileId);
        }
        return "%s/kit/file/stream/%s".formatted(trimTrailingSlash(baseUrl), fileId);
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
