package org.quyq.gwsu.security.abac.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.domain.SecurityAbac;
import org.quyq.gwsu.security.abac.domain.SecurityAbacField;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacFieldMapper;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacMapper;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacPermissionMapper;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionFieldWrapper;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionUrlWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/15
 * @description 权限改变管理器
 */
@Component
@RequiredArgsConstructor
public class PermissionAlterationManager {

    private final ObjectProvider<List<IAbacAlterationProvider>> providers;

    private final ISecurityAbacService abacService;

    @Resource
    private SecurityAbacMapper abacMapper;

    @Resource
    private SecurityAbacPermissionMapper urlPermissionMapper;

    @Resource
    private SecurityAbacFieldMapper fieldPermissionMapper;

    /**
     * 变更url权限
     *
     * @param type
     * @param context
     */
    public void alterationUrlPermission(@NonNull AbacPerType type, @NonNull ExpressionContext context) {
        List<IAbacAlterationProvider> providerss = this.providers.getIfAvailable();
        if (Objects.isNull(providerss)) {
            return;
        }

        providerss.stream().filter(v -> type == v.abacType()).findFirst()
                .ifPresent(provider -> {
                    String expression = provider.buildExpression(context);
                    AbacPermissionUrlWrapper wrapper = AbacPermissionUrlWrapper.builder(expression)
                            .abacMapper(abacMapper)
                            .urlPermissionMapper(urlPermissionMapper)
                            .build();
                    provider.alterationUrlPermission(context, wrapper);
                    //重新加载url权限策略
                    abacService.syncPolicies();
                    removeAbacIfNoPermission(expression);
                });

    }


    /**
     * 变更字段权限
     *
     * @param type
     * @param context
     */
    public void alterationFieldsPermission(@NonNull AbacPerType type,@NonNull ExpressionContext context) {

        List<IAbacAlterationProvider> providerss = this.providers.getIfAvailable();
        if (Objects.isNull(providerss)) {
            return;
        }

        providerss.stream().filter(v -> type == v.abacType()).findFirst()
                .ifPresent(provider -> {
                    String expression = provider.buildExpression(context);
                    AbacPermissionFieldWrapper wrapper = AbacPermissionFieldWrapper.builder(expression)
                            .abacMapper(abacMapper)
                            .fieldPermissionMapper(fieldPermissionMapper)
                            .build();
                    provider.alterationFieldPermission(context, wrapper);
                    //重新加载字段权限策略
                    abacService.syncFieldPolicies();
                    removeAbacIfNoPermission(expression);
                });

    }


    private void removeAbacIfNoPermission(String expression){
        SecurityAbac securityAbac = abacMapper.selectOne(new LambdaQueryWrapper<SecurityAbac>()
                .eq(SecurityAbac::getExpression, expression));

        if(Objects.isNull(securityAbac)){
            return;
        }

        String id = securityAbac.getId();

        Long urlPCount = urlPermissionMapper.selectCount(new LambdaQueryWrapper<SecurityAbacPermission>()
                .eq(SecurityAbacPermission::getAbacId, id));

        Long fieldPCount = fieldPermissionMapper.selectCount(new LambdaQueryWrapper<SecurityAbacField>()
                .eq(SecurityAbacField::getAbacId, id));

        if(urlPCount == 0 && fieldPCount == 0){
            abacMapper.deleteById(id);
        }

    }

}
