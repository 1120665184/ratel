package org.quyq.gwsu.common.security.config;

import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Security 模块运行时提示注册器，用于 AOT 编译
 * <p>
 * 注册 VisitorDeserializer 中反射访问的类及其字段
 *
 * @author Quyq
 * @date 2026/4/12
 */
public class SecurityRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        // 注册 UserInfo.DefaultUserInfo 及其继承链的所有字段访问
        registerClassWithInheritance(hints, UserInfo.DefaultUserInfo.class);

        // 注册 ClientInfo.DefaultClientInfo 及其继承链的所有字段访问
        registerClassWithInheritance(hints, ClientInfo.DefaultClientInfo.class);
    }

    /**
     * 注册类及其所有父类的反射提示
     *
     * @param hints 运行时提示
     * @param clazz 目标类
     */
    private void registerClassWithInheritance(RuntimeHints hints, Class<?> clazz) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            // 注册类的字段访问，使用 ACCESS_DECLARED_FIELDS 替代已弃用的 DECLARED_FIELDS
            hints.reflection()
                    .registerType(currentClass, MemberCategory.ACCESS_DECLARED_FIELDS);
            currentClass = currentClass.getSuperclass();
        }
    }
}
