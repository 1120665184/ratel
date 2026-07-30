package org.quyq.gwsu.common.security.utils;


import cn.hutool.core.util.NumberUtil;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.api.IConfigInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description 配置信息获取工具类
 */
public class ConfigInfoUtils {

    private static final Pattern CONFIG_PLACEHOLDER_PATTERN = Pattern.compile("<([^<>:]+)(?::([^<>]+))?>");

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

    /**
     * 替换文本中的配置占位符。
     * <p>
     * {@code <config_key>} 使用配置原始值；
     * {@code <config_key:field>} 和 {@code <config_key:field.field>} 将配置值解析为 JSON 后读取指定字段。
     *
     * @param source 待处理文本
     * @return 替换配置占位符后的文本
     */
    public static String replaceConfigPlaceholders(String source) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        Matcher matcher = CONFIG_PLACEHOLDER_PATTERN.matcher(source);
        Set<String> configKeys = new LinkedHashSet<>();
        while (matcher.find()) {
            configKeys.add(matcher.group(1));
        }
        if (configKeys.isEmpty()) {
            return source;
        }

        Map<String, ConfigVO> configs = keys(new ArrayList<>(configKeys));
        matcher.reset();
        StringBuilder result = new StringBuilder(source.length());
        while (matcher.find()) {
            String configKey = matcher.group(1);
            String fieldPath = matcher.group(2);
            ConfigVO config = Optional.ofNullable(configs.get(configKey))
                    .orElseThrow(() -> new SecurityException("没有【%s】配置，请检查".formatted(configKey)));
            String replacement = fieldPath == null
                    ? config.getConfigValue()
                    : readJsonField(configKey, config.getConfigValue(), fieldPath);
            if (replacement == null) {
                throw new SecurityException("配置【%s】的值为空，请检查".formatted(configKey));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String readJsonField(String configKey, String configValue, String fieldPath) {
        JsonNode current;
        try {
            current = objectMapper.readTree(configValue);
        } catch (Exception exception) {
            throw new SecurityException("配置【%s】不是有效的JSON，无法读取字段【%s】"
                    .formatted(configKey, fieldPath));
        }
        for (String field : fieldPath.split("\\.")) {
            current = current == null ? null : current.get(field);
            if (current == null) {
                throw new SecurityException("配置【%s】中不存在字段【%s】，请检查"
                        .formatted(configKey, fieldPath));
            }
        }
        return current.isString() ? current.asString() : current.toString();
    }

    private static Map<String, ConfigVO> keys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }
        IConfigInfoClientApi api = SpringUtils.getBean(IConfigInfoClientApi.class);

        return FeignUtils.data(api.getByKeys(keys));

    }


}
