package org.quyq.gwsu.common.security.utils;


import cn.hutool.core.util.NumberUtil;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.api.IConfigInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.NumberUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description 配置信息获取工具类
 */
public class ConfigInfoUtils {

    public ConfigInfoUtils(ObjectMapper objectMapper) {
        ConfigInfoUtils.objectMapper = objectMapper;
    }


    private static ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);


    public static String get(String key) {
        Map<String, ConfigVO> values = keys(Collections.singletonList(key));
        return Optional.ofNullable(values.get(key))
                .map(ConfigVO::getConfigValue)
                .orElse(null);
    }

    public static Map<String, String> get(List<String> keys) {
        Map<String, String> finV = new HashMap<>(keys.size());
        keys(keys).forEach((k, v) -> finV.put(k, v.getConfigValue()));
        return finV;
    }

    /**
     * 获取配置转成Bool类型
     *
     * @param key
     * @return
     */
    public static boolean getByBoolean(String key) {
        Map<String, ConfigVO> values = keys(Collections.singletonList(key));
        return Optional.ofNullable(values.get(key))
                .filter(v -> 3 == v.getConfigType())
                .map(ConfigVO::getConfigValue)
                .map(Boolean::parseBoolean)
                .orElseThrow(() -> new SecurityException("没有【%s】配置或者配置类型不是bool类型，请检查".formatted(key)));
    }

    public static Number getByNumber(String key) {

        Map<String, ConfigVO> values = keys(Collections.singletonList(key));
        return Optional.ofNullable(values.get(key))
                .filter(v -> 2 == v.getValueType())
                .map(ConfigVO::getConfigValue)
                .map(NumberUtil::parseNumber)
                .orElseThrow(() -> new SecurityException("没有【%s】配置或者配置类型不是数值类型，请检查".formatted(key)));
    }

    /**
     * 获取类型成数值
     *
     * @param key
     * @param numType
     * @param <T>
     * @return
     */
    public static <T extends Number> T getByNumber(String key, Class<T> numType) {
        Map<String, ConfigVO> values = keys(Collections.singletonList(key));
        return Optional.ofNullable(values.get(key))
                .filter(v -> 2 == v.getValueType())
                .map(ConfigVO::getConfigValue)
                .map(v -> NumberUtils.parseNumber(v, numType))
                .orElseThrow(() -> new SecurityException("没有【%s】配置或者配置类型不是数值类型，请检查".formatted(key)));
    }

    /**
     * 将数据转为指定对象
     *
     * @param key
     * @param objType
     * @param <T>
     * @return
     */
    public static <T> T getByObject(String key, Class<T> objType) {

        Map<String, ConfigVO> values = keys(Collections.singletonList(key));
        return Optional.ofNullable(values.get(key))
                .filter(v -> 4 == v.getValueType())
                .map(ConfigVO::getConfigValue)
                .map(v -> objectMapper.readValue(v, objType))
                .orElseThrow(() -> new SecurityException("没有【%s】配置或者配置类型不是对象类型，请检查".formatted(key)));
    }

    /**
     * 将数据转为指定对象
     *
     * @param key
     * @param objType
     * @param <T>
     * @return
     */
    public static <T> T getByObject(String key, TypeReference<T> objType) {
        Map<String, ConfigVO> values = keys(Collections.singletonList(key));
        return Optional.ofNullable(values.get(key))
                .filter(v -> 4 == v.getValueType())
                .map(ConfigVO::getConfigValue)
                .map(v -> objectMapper.readValue(v, objType))
                .orElseThrow(() -> new SecurityException("没有【%s】配置或者配置类型不是对象类型，请检查".formatted(key)));
    }


    private static Map<String, ConfigVO> keys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }
        IConfigInfoClientApi api = SpringUtils.getBean(IConfigInfoClientApi.class);

        return FeignUtils.data(api.getByKeys(keys));

    }


}
