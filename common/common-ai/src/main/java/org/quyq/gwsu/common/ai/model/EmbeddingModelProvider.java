package org.quyq.gwsu.common.ai.model;

import org.quyq.gwsu.common.ai.config.properties.ModelEmbeddingConfigDTO;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Objects;

/**
 * 向量化模型提供入口。
 */
public class EmbeddingModelProvider {

    public static final String MODEL_EMBEDDING_CONFIG = "model_embedding_config";

    private static ModelEmbeddingConfigDTO CONFIG;

    private static EmbeddingModel MODEL;

    public static EmbeddingModel generateModel() {
        ModelEmbeddingConfigDTO newConfig = ConfigInfoUtils.getByObject(MODEL_EMBEDDING_CONFIG, ModelEmbeddingConfigDTO.class);
        if (configChange(newConfig)) {
            createModel(newConfig);
        }
        return MODEL;
    }

    private static void createModel(ModelEmbeddingConfigDTO config) {
        synchronized (EmbeddingModelProvider.class) {
            if (configChange(config)) {
                CONFIG = config;
                MODEL = EmbeddingModelProviderType.createModelFromConfig(config);
            }
        }
    }

    private static boolean configChange(ModelEmbeddingConfigDTO newConfig) {
        if (Objects.isNull(CONFIG) || Objects.isNull(MODEL)) {
            return true;
        }
        return !Objects.equals(newConfig, CONFIG);
    }
}
