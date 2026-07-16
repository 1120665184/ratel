package org.quyq.gwsu.common.ai.model;


import io.agentscope.core.model.Model;
import org.quyq.gwsu.common.ai.config.properties.ModelLlmConfigDTO;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;

import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/31
 * @description 模型提供
 */
public class ModelProvider {


    private static ModelLlmConfigDTO CONFIG;

    private static Model MODEL;

    public final static String MODEL_LLM_CONFIG = "model_llm_config";


    /**
     * 获取模型
     *
     * @return
     */
    public static Model generateModel() {

        ModelLlmConfigDTO newConfig = ConfigInfoUtils.getByObject(MODEL_LLM_CONFIG, ModelLlmConfigDTO.class);
        if (configChange(newConfig)) {
            createModel(newConfig);
        }


        return MODEL;

    }


    private static void createModel(ModelLlmConfigDTO config) {
        synchronized (ModelProvider.class) {
            if (configChange(config)) {
                CONFIG = config;
                MODEL = ModelProviderType.createModelFromConfig(config);

            }
        }
    }

    //校验系统配置是否变更
    private static boolean configChange(ModelLlmConfigDTO newConfig) {
        if (Objects.isNull(CONFIG) || Objects.isNull(MODEL)) {
            return true;
        }
        return !Objects.equals(newConfig, CONFIG);

    }

}
