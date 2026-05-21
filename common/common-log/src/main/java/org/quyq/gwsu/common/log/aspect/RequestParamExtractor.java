package org.quyq.gwsu.common.log.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 请求参数提取器，使用 Jackson 3 实现参数序列化与过滤
 * <p>
 * 替代原 Fastjson 的 PropertyFilter + JSONObject 实现，
 * 通过 ValueSerializerModifier 在序列化时过滤掉 MultipartFile、HttpServletRequest 等不需要记录的属性类型
 * </p>
 *
 * @author Quyq
 */
public class RequestParamExtractor {

    private static final String ARGS_KEY = "args";

    private final ObjectMapper filteringMapper;

    public RequestParamExtractor() {
        SimpleModule filterModule = new SimpleModule("requestParamFilter");
        filterModule.setSerializerModifier(new FilterValueSerializerModifier());

        this.filteringMapper = JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .addModule(filterModule)
                .build();
    }

    /**
     * 提取请求参数，返回包含方法参数和查询参数的JSON节点
     *
     * @param request    HTTP请求
     * @param invocation 方法调用信息
     * @return 包含args和query的JsonNode
     */
    public JsonNode getRequestParam(HttpServletRequest request, MethodInvocation invocation) {
        Object[] arguments = invocation.getArguments();
        ObjectNode allParams = filteringMapper.createObjectNode();
        ObjectNode argsNode = filteringMapper.createObjectNode();

        for (int i = 0; i < arguments.length; i++) {
            Object param = arguments[i];
            if (Objects.isNull(param) || isFilterObject(param)) {
                continue;
            }
            try {
                JsonNode paramNode = filteringMapper.valueToTree(param);
                argsNode.set(String.valueOf(i), paramNode);
            } catch (Exception ex) {
                argsNode.put(String.valueOf(i), String.valueOf(param));
            }
        }

        allParams.set(ARGS_KEY, argsNode);
        allParams.set("params", filteringMapper.valueToTree(request.getParameterMap()));

        return allParams;
    }

    /**
     * 判断对象是否为需要过滤的类型（不参与参数序列化）
     *
     * @param o 待检查对象
     * @return 是否需要过滤
     */
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection<?> collection = (Collection<?>) o;
            Iterator<?> iter = collection.iterator();
            return !collection.isEmpty() && iter.next() instanceof MultipartFile;
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<?, ?> map = (Map<?, ?>) o;
            if (map.isEmpty()) {
                return false;
            }
            Iterator<?> iter = map.entrySet().iterator();
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            return entry.getValue() instanceof MultipartFile;
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }

    /**
     * 值序列化修改器，在序列化时过滤掉不需要的属性类型
     * <p>
     * 等效于 Fastjson 的 PropertyFilter 功能，通过检查属性的声明类型，
     * 移除 MultipartFile、HttpServletRequest、HttpServletResponse、BindingResult 及其集合/数组/Map形式
     * </p>
     */
    private static class FilterValueSerializerModifier extends ValueSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
            return beanProperties.stream()
                    .filter(pw -> !isFilterType(pw.getType()))
                    .collect(Collectors.toList());
        }

        /**
         * 判断属性类型是否需要过滤
         */
        private boolean isFilterType(JavaType type) {
            Class<?> rawClass = type.getRawClass();

            // 直接匹配过滤类型
            if (MultipartFile.class.isAssignableFrom(rawClass)
                    || HttpServletRequest.class.isAssignableFrom(rawClass)
                    || HttpServletResponse.class.isAssignableFrom(rawClass)
                    || BindingResult.class.isAssignableFrom(rawClass)) {
                return true;
            }

            // 数组元素为过滤类型（如 MultipartFile[]）
            if (type.isArrayType()) {
                JavaType contentType = type.getContentType();
                return contentType != null && MultipartFile.class.isAssignableFrom(contentType.getRawClass());
            }

            // 集合元素为过滤类型（如 List<MultipartFile>）
            if (type.isCollectionLikeType()) {
                JavaType contentType = type.getContentType();
                return contentType != null && MultipartFile.class.isAssignableFrom(contentType.getRawClass());
            }

            // Map值为过滤类型（如 Map<String, MultipartFile>）
            if (type.isMapLikeType()) {
                JavaType valueType = type.getContentType();
                return valueType != null && MultipartFile.class.isAssignableFrom(valueType.getRawClass());
            }

            return false;
        }
    }

}
