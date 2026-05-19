package org.quyq.gwsu.security.abac.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.quyq.gwsu.security.abac.domain.SecurityAbac;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacMapper;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacPermissionMapper;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Quyq
 * @date 2026/4/15
 * @description
 */
public class AbacPermissionUrlWrapper {

    private final String expression;
    private final AtomicReference<SecurityAbac> abacExpression = new AtomicReference<>();
    private final SecurityAbacMapper abacMapper;
    private final SecurityAbacPermissionMapper permissionMapper;

    private AbacPermissionUrlWrapper(String expression, SecurityAbacMapper abacMapper, SecurityAbacPermissionMapper permissionMapper) {
        this.expression = expression;
        this.abacMapper = abacMapper;
        this.permissionMapper = permissionMapper;
    }

    /**
     * 获取abac表达式
     *
     * @return
     */
    public Optional<SecurityAbac> abac() {
        if (Objects.nonNull(abacExpression.get())) {
            return Optional.of(abacExpression.get());
        }
        LambdaQueryWrapper<SecurityAbac> wrapper = new LambdaQueryWrapper<SecurityAbac>()
                .eq(SecurityAbac::getExpression, expression)
                .eq(SecurityAbac::getStatus, true)
                .eq(SecurityAbac::getDeleted, false);
        SecurityAbac securityAbac = abacMapper.selectOne(wrapper);
        if (Objects.nonNull(securityAbac)) {
            abacExpression.set(securityAbac);
        }

        return Optional.ofNullable(securityAbac);
    }

    /**
     * 获取该abac的所有权限列表
     *
     * @return
     */
    public List<SecurityAbacPermission> allPermissions() {
        return abac()
                .map(v -> {
                    LambdaQueryWrapper<SecurityAbacPermission> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(SecurityAbacPermission::getAbacId, v.getId())
                            .eq(SecurityAbacPermission::getStatus, true)
                            .eq(SecurityAbacPermission::getDeleted, false);

                    return permissionMapper.selectList(wrapper);
                }).orElseGet(Collections::emptyList);
    }

    /**
     * 删除该表达式的所有权限
     */
    public void removeAll() {
        abac().ifPresent(v ->
                permissionMapper.delete(new LambdaQueryWrapper<SecurityAbacPermission>().eq(SecurityAbacPermission::getAbacId, v.getId()))
        );
    }

    /**
     * 删除指定ID的权限
     */
    public void removeByIds(List<String> ids) {
        abac().ifPresent(abac ->
                permissionMapper.delete(new LambdaQueryWrapper<SecurityAbacPermission>()
                        .eq(SecurityAbacPermission::getAbacId, abac.getId())
                        .in(SecurityAbacPermission::getId, ids)
                )
        );
    }

    /**
     * 添加权限
     *
     * @param permissions
     */
    public void addPermissions(List<SecurityAbacPermission> permissions) {
        if(CollectionUtils.isEmpty(permissions)) {
            return;
        }
        SecurityAbac abac = abac().orElseGet(this::buildAbac);
        permissions.forEach(permission -> permission.setAbacId(abac.getId()));

        permissionMapper.insert(permissions);
    }

    private SecurityAbac buildAbac() {
        SecurityAbac abac = new SecurityAbac();
        abac.setExpression(expression);
        abac.setStatus(true);
        abacMapper.insert(abac);
        abacExpression.set(abac);
        return abac;
    }

    /**
     * 替换该表达式的权限
     * 删除旧权限，添加信息权限
     *
     * @param permissions
     */
    public void replacePermission(List<SecurityAbacPermission> permissions) {
        removeAll();
        if(CollectionUtils.isEmpty(permissions)) {
            return;
        }
        SecurityAbac abac = abac().orElseGet(this::buildAbac);
        permissions.forEach(permission -> permission.setAbacId(abac.getId()));
        permissionMapper.insert(permissions);
    }

    public static AbacPermissionWrapperBuilder builder(String expression) {
        return new AbacPermissionWrapperBuilder(expression);
    }

    public static class AbacPermissionWrapperBuilder {

        private final String expression;
        private SecurityAbacMapper abacMapper;
        private SecurityAbacPermissionMapper permissionMapper;

        public AbacPermissionWrapperBuilder(String expression) {
            this.expression = expression;
        }

        public AbacPermissionWrapperBuilder abacMapper(SecurityAbacMapper abacMapper) {
            this.abacMapper = abacMapper;
            return this;
        }

        public AbacPermissionWrapperBuilder urlPermissionMapper(SecurityAbacPermissionMapper permissionMapper) {
            this.permissionMapper = permissionMapper;
            return this;
        }

        public AbacPermissionUrlWrapper build() {
            return new AbacPermissionUrlWrapper(expression, abacMapper, permissionMapper);
        }

    }

}
