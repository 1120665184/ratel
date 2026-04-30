package org.quyq.gwsu.security.abac.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.quyq.gwsu.security.abac.domain.SecurityAbac;
import org.quyq.gwsu.security.abac.domain.SecurityAbacField;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacFieldMapper;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacMapper;

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
public class AbacPermissionFieldWrapper {

    private final String expression;
    private final AtomicReference<SecurityAbac> abacExpression = new AtomicReference<>();
    private final SecurityAbacMapper abacMapper;
    private final SecurityAbacFieldMapper fieldMapper;

    public AbacPermissionFieldWrapper(String expression, SecurityAbacMapper abacMapper, SecurityAbacFieldMapper fieldMapper) {
        this.expression = expression;
        this.abacMapper = abacMapper;
        this.fieldMapper = fieldMapper;
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

    private SecurityAbac buildAbac() {
        SecurityAbac abac = new SecurityAbac();
        abac.setExpression(expression);
        abac.setStatus(true);
        abacMapper.insert(abac);
        abacExpression.set(abac);
        return abac;
    }

    /**
     * 获取该表达式所有的接口权限
     *
     * @return
     */
    public List<SecurityAbacField> allPermission() {
        return abac()
                .map(abac -> {
                    LambdaQueryWrapper<SecurityAbacField> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(SecurityAbacField::getAbacId, abac.getId())
                            .eq(SecurityAbacField::getStatus, true)
                            .eq(SecurityAbacField::getDeleted, false);
                    return fieldMapper.selectList(wrapper);
                }).orElseGet(Collections::emptyList);
    }

    /**
     * 删除该表达式的所有权限
     */
    public void removeAll() {
        abac().ifPresent(v ->
                fieldMapper.delete(new LambdaQueryWrapper<SecurityAbacField>().eq(SecurityAbacField::getAbacId, v.getId()))
        );
    }

    /**
     * 删除指定ID的权限
     */
    public void removeByIds(List<Long> ids) {
        abac().ifPresent(abac ->
                fieldMapper.delete(new LambdaQueryWrapper<SecurityAbacField>()
                        .eq(SecurityAbacField::getAbacId, abac.getId())
                        .in(SecurityAbacField::getId, ids)
                )
        );
    }

    /**
     * 添加权限
     *
     * @param permissions
     */
    public void addPermissions(List<SecurityAbacField> permissions) {
        SecurityAbac abac = abac().orElseGet(this::buildAbac);
        permissions.forEach(permission -> permission.setAbacId(abac.getId()));
        fieldMapper.insert(permissions);
    }

    /**
     * 替换该表达式的权限
     * 删除旧权限，添加信息权限
     *
     * @param permissions
     */
    public void replacePermission(List<SecurityAbacField> permissions) {
        SecurityAbac abac = abac().orElseGet(this::buildAbac);
        permissions.forEach(permission -> permission.setAbacId(abac.getId()));
        removeAll();
        fieldMapper.insert(permissions);
    }

    public static AbacPermissionFieldWrapperBuilder builder(String expression) {
        return new AbacPermissionFieldWrapperBuilder(expression);
    }

    public static class AbacPermissionFieldWrapperBuilder {
        private final String expression;
        private SecurityAbacMapper abacMapper;
        private SecurityAbacFieldMapper fieldMapper;

        public AbacPermissionFieldWrapperBuilder(String expression) {
            this.expression = expression;
        }

        public AbacPermissionFieldWrapperBuilder abacMapper(SecurityAbacMapper abacMapper) {
            this.abacMapper = abacMapper;
            return this;
        }

        public AbacPermissionFieldWrapperBuilder fieldPermissionMapper(SecurityAbacFieldMapper fieldMapper) {
            this.fieldMapper = fieldMapper;
            return this;
        }

        public AbacPermissionFieldWrapper build() {
            return new AbacPermissionFieldWrapper(expression, abacMapper, fieldMapper);
        }

    }


}
