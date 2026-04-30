package org.quyq.gwsu.common.security.domain.deserializer;


import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Quyq
 * @date 2026/4/11
 * @description
 */
public class JacksonCompatibleTypeAdapterFactory implements TypeAdapterFactory {
    private final String classProperty = "@class";
    private final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<?> rawType = typeToken.getRawType();
        // 基本类型和字符串不需要处理
        if (rawType.isPrimitive() || rawType == String.class) return null;

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, typeToken);
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                // 序列化按原样委托（如需模拟Jackson输出可扩展）
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonElement element = JsonParser.parseReader(in);
                // 1. 处理对象多态：{"@class":"...", ...}
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has(classProperty)) {
                        return deserializeWithClass(gson, element, obj.get(classProperty).getAsString(), typeToken);
                    }
                }
                // 2. 处理集合多态：["typeName", [actualElements]]
                else if (element.isJsonArray()) {
                    JsonArray array = element.getAsJsonArray();
                    if (array.size() == 2 && array.get(0).isJsonPrimitive() && array.get(1).isJsonArray()) {
                        String className = array.get(0).getAsString();
                        // 检查是否是集合类型
                        if (className.startsWith("java.util.") || className.startsWith("com.google.common.collect.")) {
                            // 提取实际数据数组，并递归反序列化
                            JsonElement actualData = array.get(1);
                            // 尝试使用原始目标类型反序列化实际数据
                            return gson.fromJson(actualData, typeToken.getType());
                        }
                    }
                }
                // 3. 默认处理
                return delegate.fromJsonTree(element);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeWithClass(Gson gson, JsonElement element, String className, TypeToken<T> expectedType) {
        try {
            Class<?> clazz = loadClass(className);
            if (!expectedType.getRawType().isAssignableFrom(clazz)) {
                throw new JsonParseException("Type " + className + " not assignable to " + expectedType.getRawType());
            }
            // 使用 getDelegateAdapter 避免递归调用当前 TypeAdapterFactory
            TypeToken<?> targetToken = TypeToken.get(clazz);
            TypeAdapter<?> adapter = gson.getDelegateAdapter(this, targetToken);
            return (T) adapter.fromJsonTree(element);
        } catch (ClassNotFoundException e) {
            // 类加载失败时，移除 @class 属性后降级处理，让自定义反序列化器处理
            JsonObject obj = element.getAsJsonObject();
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (!classProperty.equals(entry.getKey())) {
                    copy.add(entry.getKey(), entry.getValue());
                }
            }
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, expectedType);
            return delegate.fromJsonTree(copy);
        }
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> clazz = classCache.get(className);
        if (clazz != null) {
            return clazz;
        }
        clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        classCache.put(className, clazz);
        return clazz;
    }
}
