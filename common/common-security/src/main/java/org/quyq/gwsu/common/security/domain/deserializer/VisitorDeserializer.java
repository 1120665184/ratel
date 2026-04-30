package org.quyq.gwsu.common.security.domain.deserializer;

import com.google.gson.*;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Visitor 类型的自定义反序列化器
 * <p>
 * 反序列化逻辑：
 * 1. 如果存在 @class 属性，使用默认的多态类型解析
 * 2. 如果不存在 @class 属性，根据 userId 属性判断：
 * - 存在 userId -> 反序列化为 UserInfo.DefaultUserInfo
 * - 不存在 userId -> 反序列化为 ClientInfo.DefaultClientInfo
 * 3. 降级转换时，将目标类中不存在的字段放入 properties 中
 *
 * @author Quyq
 * @date 2026/4/10
 */
public class VisitorDeserializer implements JsonDeserializer<Visitor> {

    private static final String TYPE_PROPERTY = "@class";
    private static final String USER_ID_PROPERTY = "userId";

    /**
     * UserInfo.DefaultUserInfo 的已知字段缓存（通过反射自动获取，包括继承字段）
     */
    private static final Set<String> USER_INFO_FIELDS = getAllFields(UserInfo.DefaultUserInfo.class);

    /**
     * ClientInfo.DefaultClientInfo 的已知字段缓存（通过反射自动获取，包括继承字段）
     */
    private static final Set<String> CLIENT_INFO_FIELDS = getAllFields(ClientInfo.DefaultClientInfo.class);

    @Override
    public Visitor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        // 如果存在 @class 属性，尝试使用类型信息进行反序列化
        if (obj.has(TYPE_PROPERTY)) {
            String className = obj.get(TYPE_PROPERTY).getAsString();
            try {
                return deserializeByType(context, obj, className);
            } catch (ClassNotFoundException e) {
                // 类不存在时，降级通过 userId 属性判断类型
                return deserializeByUserId(context, obj);
            } catch (ClassCastException e) {
                throw new JsonParseException("Invalid type for Visitor: " + className, e);
            }
        }

        // 根据 userId 属性判断具体类型
        return deserializeByUserId(context, obj);
    }

    /**
     * 根据 @class 属性指定的类型进行反序列化
     */
    private Visitor deserializeByType(JsonDeserializationContext context, JsonObject obj, String className)
            throws ClassNotFoundException, ClassCastException {
        Class<?> clazz = Class.forName(className);
        if (!Visitor.class.isAssignableFrom(clazz)) {
            throw new ClassCastException("Class " + className + " is not assignable to Visitor");
        }
        @SuppressWarnings("unchecked")
        Class<? extends Visitor> visitorClass = (Class<? extends Visitor>) clazz;
        return context.deserialize(obj, visitorClass);
    }

    /**
     * 根据 userId 属性判断具体类型，并将多余字段放入 properties
     */
    private Visitor deserializeByUserId(JsonDeserializationContext context, JsonObject obj) {
        if (obj.has(USER_ID_PROPERTY)) {
            UserInfo.DefaultUserInfo userInfo = context.deserialize(obj, UserInfo.DefaultUserInfo.class);
            Map<String, Object> extraProperties = extractExtraProperties(obj, USER_INFO_FIELDS);
            if (!extraProperties.isEmpty()) {
                userInfo.setPrototype(extraProperties);
            }
            return userInfo;
        } else {
            ClientInfo.DefaultClientInfo clientInfo = context.deserialize(obj, ClientInfo.DefaultClientInfo.class);
            Map<String, Object> extraProperties = extractExtraProperties(obj, CLIENT_INFO_FIELDS);
            if (!extraProperties.isEmpty()) {
                clientInfo.setPrototype(extraProperties);
            }
            return clientInfo;
        }
    }

    /**
     * 提取 JSON 中存在但目标类中不存在的字段
     *
     * @param obj         JSON 对象
     * @param knownFields 目标类的已知字段集合
     * @return 额外字段的 Map
     */
    private Map<String, Object> extractExtraProperties(JsonObject obj, Set<String> knownFields) {
        Map<String, Object> extraProperties = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            // 跳过已知字段和类型属性
            if (!knownFields.contains(key) && !TYPE_PROPERTY.equals(key)) {
                extraProperties.put(key, contextDeserialize(entry.getValue()));
            }
        }
        return extraProperties;
    }

    /**
     * 递归反序列化 JSON 元素，处理嵌套对象和数组
     */
    private Object contextDeserialize(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else {
                return primitive.getAsString();
            }
        } else if (element.isJsonArray()) {
            return element.getAsJsonArray().asList().stream()
                    .map(this::contextDeserialize)
                    .toList();
        } else if (element.isJsonObject()) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), contextDeserialize(entry.getValue()));
            }
            return map;
        }
        return null;
    }

    /**
     * 通过反射获取类及其所有父类的字段名称
     * <p>
     * 自动适应字段扩展，无需手动维护字段列表
     *
     * @param clazz 目标类
     * @return 不可变的字段名称集合
     */
    private static Set<String> getAllFields(Class<?> clazz) {
        Set<String> fieldNames = new HashSet<>();
        Class<?> currentClass = clazz;
        // 遍历类继承链，直到 Object
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                fieldNames.add(field.getName());
            }
            currentClass = currentClass.getSuperclass();
        }
        return Collections.unmodifiableSet(fieldNames);
    }
}
