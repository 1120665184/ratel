package org.quyq.gwsu.common.ai.config.properties;

import lombok.Data;

/**
 * 重排模型配置 DTO，与前端 model_rerank_config JSON 结构一一映射。
 */
@Data
public class ModelRerankConfigDTO {

    /**
     * 当前激活的重排模型提供商。
     *
     * <p>当前支持值：dashscope
     */
    private String provider;

    private DashscopeRerankConfigDTO dashscope = new DashscopeRerankConfigDTO();

    @Data
    public static class DashscopeRerankConfigDTO {

        private String apiKey;

        private String modelName;

        private String baseUrl;

        private Integer topN = 10;

        private Boolean returnDocuments = true;
    }
}
