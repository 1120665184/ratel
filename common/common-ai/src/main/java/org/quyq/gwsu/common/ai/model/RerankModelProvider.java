package org.quyq.gwsu.common.ai.model;

import com.alibaba.cloud.ai.model.RerankModel;
import org.quyq.gwsu.common.ai.config.properties.ModelRerankConfigDTO;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * 重排模型提供入口。
 */
public class RerankModelProvider {

    public static final String MODEL_RERANK_CONFIG = "model_rerank_config";

    private static ModelRerankConfigDTO CONFIG;

    private static RerankModel MODEL;

    public static Optional<RerankModel> generateModel() {
        ModelRerankConfigDTO newConfig = ConfigInfoUtils.getByObject(MODEL_RERANK_CONFIG, ModelRerankConfigDTO.class);
        if (!isEnabled(newConfig) || !isConfigReady(newConfig)) {
            CONFIG = newConfig;
            MODEL = null;
            return Optional.empty();
        }
        if (configChange(newConfig)) {
            createModel(newConfig);
        }
        return Optional.ofNullable(MODEL);
    }

    private static void createModel(ModelRerankConfigDTO config) {
        synchronized (RerankModelProvider.class) {
            if (configChange(config)) {
                CONFIG = config;
                MODEL = RerankModelProviderType.createModelFromConfig(config);
            }
        }
    }

    private static boolean configChange(ModelRerankConfigDTO newConfig) {
        if (Objects.isNull(CONFIG) || Objects.isNull(MODEL)) {
            return true;
        }
        return !Objects.equals(newConfig, CONFIG);
    }

    private static boolean isEnabled(ModelRerankConfigDTO config) {
        return config != null && !Boolean.FALSE.equals(config.getEnabled());
    }

    private static boolean isConfigReady(ModelRerankConfigDTO config) {
        return config != null
                && StringUtils.hasText(config.getProvider())
                && "dashscope".equalsIgnoreCase(config.getProvider())
                && config.getDashscope() != null
                && StringUtils.hasText(config.getDashscope().getApiKey())
                && StringUtils.hasText(config.getDashscope().getModelName());
    }
}
