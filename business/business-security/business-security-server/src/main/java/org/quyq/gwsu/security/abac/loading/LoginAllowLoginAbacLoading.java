package org.quyq.gwsu.security.abac.loading;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.security.casbin.function.IsUserLoginFunction;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.service.IAbacAlterationProvider;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionUrlWrapper;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiResourceService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/17
 * @description 从API资源中加载接口权限
 */
@Component
@RequiredArgsConstructor
public class LoginAllowLoginAbacLoading implements IAbacAlterationProvider {

    private final ISecurityApiResourceService apiResourceService;

    @Override
    public AbacPerType abacType() {
        return AbacPerType.API_RESOURCE;
    }

    @Override
    public String buildExpression(ExpressionContext context) {
        return "%s()".formatted(IsUserLoginFunction.NAME);
    }


    @Override
    public void alterationUrlPermission(ExpressionContext context, AbacPermissionUrlWrapper wrapper) {
        List<SecurityAbacPermission> permissions = apiResourceService.lambdaQuery()
                .eq(SecurityApiResource::getLoginAllowAccess, true)
                .list()
                .stream()
                .map(v -> {
                    SecurityAbacPermission permission = new SecurityAbacPermission();
                    permission.setEffect(AbacEffect.PERMIT)
                            .setStatus(true)
                            .setAction(v.getReqMethod())
                            .setUrlPattern(v.getReqPath())
                            .setResourceType(v.getModulePrefix());
                    return permission;

                }).toList();

        wrapper.replacePermission(permissions);
    }
}
