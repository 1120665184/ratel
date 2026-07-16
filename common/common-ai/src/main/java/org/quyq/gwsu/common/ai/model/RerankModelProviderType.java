package org.quyq.gwsu.common.ai.model;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.model.RerankModel;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.config.properties.ModelRerankConfigDTO;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

/**
 * 重排模型提供商工厂。
 */
public enum RerankModelProviderType {
    DASHSCOPE("dashscope") {
        @Override
        protected RerankModel createModel(ModelRerankConfigDTO config) {
            ModelRerankConfigDTO.DashscopeRerankConfigDTO c = config.getDashscope();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("DashScope rerank API Key must be configured");
            }
            DashScopeApi.Builder apiBuilder = DashScopeApi.builder().apiKey(c.getApiKey());
            if (StringUtils.hasText(c.getBaseUrl())) {
                apiBuilder.baseUrl(c.getBaseUrl());
            }
            DashScopeRerankOptions options = DashScopeRerankOptions.builder()
                    .model(c.getModelName())
                    .topN(c.getTopN())
                    .returnDocuments(c.getReturnDocuments())
                    .build();
            return new DashScopeRerankModel(apiBuilder.build(), options);
        }
    };

    private final String id;

    RerankModelProviderType(String id) {
        this.id = id;
    }

    protected abstract RerankModel createModel(ModelRerankConfigDTO config);

    public static RerankModel createModelFromConfig(ModelRerankConfigDTO config) {
        if (config == null || config.getProvider() == null) {
            throw new IllegalStateException("Rerank config or provider must not be null");
        }
        String provider = config.getProvider().trim().toLowerCase(Locale.ROOT);
        for (RerankModelProviderType type : values()) {
            if (type.id.equals(provider)) {
                return type.createModel(config);
            }
        }
        throw new IllegalStateException("Unsupported rerank config provider: " + provider);
    }
}
