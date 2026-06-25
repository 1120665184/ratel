package org.quyq.gwsu.common.ai.model;


import io.agentscope.core.model.Model;
import org.quyq.gwsu.common.ai.config.properties.AssistantConfigDTO;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;

import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/31
 * @description 模型提供
 */
public class ModelProvider {


    private static AssistantConfigDTO CONFIG;

    private static Model MODEL;

    public final static String ASSISTANT_LLM_CONFIG = "assistant_llm_config";


    /**
     * 获取模型
     *
     * @return
     */
    public static Model generateModel() {

        AssistantConfigDTO newConfig = ConfigInfoUtils.getByObject(ASSISTANT_LLM_CONFIG, AssistantConfigDTO.class);
        if (configChange(newConfig)) {
            createModel(newConfig);
        }


        return MODEL;

    }


    private static void createModel(AssistantConfigDTO config) {
        synchronized (ModelProvider.class) {
            if (configChange(config)) {
                CONFIG = config;
                MODEL = ModelProviderType.createModelFromConfig(config);

            }
        }
    }

    //校验系统配置是否变更
    private static boolean configChange(AssistantConfigDTO newConfig) {
        if (Objects.isNull(CONFIG) || Objects.isNull(MODEL)) {
            return true;
        }
        return !Objects.equals(newConfig, CONFIG);

    }

}
