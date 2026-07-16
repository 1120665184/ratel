package org.quyq.gwsu.common.ai.model;

import com.alibaba.cloud.ai.model.RerankModel;
import org.quyq.gwsu.common.ai.config.properties.ModelRerankConfigDTO;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;

import java.util.Objects;

/**
 * 重排模型提供入口。
 */
public class RerankModelProvider {

    public static final String MODEL_RERANK_CONFIG = "model_rerank_config";

    private static ModelRerankConfigDTO CONFIG;

    private static RerankModel MODEL;

    public static RerankModel generateModel() {
        ModelRerankConfigDTO newConfig = ConfigInfoUtils.getByObject(MODEL_RERANK_CONFIG, ModelRerankConfigDTO.class);
        if (configChange(newConfig)) {
            createModel(newConfig);
        }
        return MODEL;
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
}
