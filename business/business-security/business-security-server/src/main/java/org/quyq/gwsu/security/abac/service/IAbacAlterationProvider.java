package org.quyq.gwsu.security.abac.service;


import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionFieldWrapper;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionUrlWrapper;

/**
 * @author Quyq
 * @date 2026/4/15
 * @description 权限改变提供者
 */
public interface IAbacAlterationProvider {

    /**
     * 支持的abac权限类型
     *
     * @return
     */
    AbacPerType abacType();

    /**
     * 构建生成表达式逻辑
     *
     * @return
     */
    String buildExpression(ExpressionContext context);

    /**
     * url权限变更操作
     *
     * @param wrapper
     */
    default void alterationUrlPermission(ExpressionContext context  , AbacPermissionUrlWrapper wrapper) {
    }


    /**
     * 字段权限变更操作
     *
     * @param wrapper
     */
    default void alterationFieldPermission(ExpressionContext context , AbacPermissionFieldWrapper wrapper) {
    }

}
