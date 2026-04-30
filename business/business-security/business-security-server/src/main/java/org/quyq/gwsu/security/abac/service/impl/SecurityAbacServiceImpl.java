package org.quyq.gwsu.security.abac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.FieldRule;
import org.quyq.gwsu.security.abac.domain.SecurityAbac;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacMapper;
import org.quyq.gwsu.security.abac.service.ISecurityAbacService;
import org.quyq.gwsu.security.api.abac.vo.AbacFieldVO;
import org.quyq.gwsu.security.api.abac.vo.AbacPermissionVO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SecurityAbacServiceImpl extends ServiceImpl<SecurityAbacMapper, SecurityAbac> implements ISecurityAbacService {

    private final CacheUtils cacheUtils;

    @Override
    public void syncPolicies() {
        List<AbacPermissionVO> permissions = getBaseMapper().allAbacPermissions();

        List<List<String>> policies = permissions.stream()
                .map(per -> List.of(
                        "p",
                        "*",
                        per.getResourceType(),
                        per.getAction(),
                        per.getUrlPattern(),
                        per.getExpression(),
                        per.getEffect().getEffect()))
                .toList();
        cacheUtils.withRebel(() -> {
                    //设置权限数据
                    cacheUtils.set(SecurityConstants.Abac.PERMISSION_DATA_CACHE_KEY, new Gson().toJson(policies));
                    //发送通知，权限变更
                    return cacheUtils.convertAndSend(SecurityConstants.Abac.PERMISSION_CHANGE_NOTICE_TOPIC, "syncAccess");
                }

        );


    }

    @Override
    public void syncFieldPolicies() {
        List<AbacFieldVO> abacFieldVOS = getBaseMapper().allAbacFields();


        Map<String, List<FieldRule>> allRules = new HashMap<>();
        for (AbacFieldVO abacFieldVo : abacFieldVOS) {
            String key = "%s:%s".formatted(abacFieldVo.getResourceType(), abacFieldVo.getAction());

            allRules.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new FieldRule(
                            abacFieldVo.getUrlPattern(),
                            abacFieldVo.getFieldMode().getEffect(),
                            new HashSet<>(abacFieldVo.getFields()),
                            abacFieldVo.getExpression()
                    ));

        }

        cacheUtils.withRebel(() -> {
                    //设置权限数据
                    cacheUtils.set(SecurityConstants.Abac.PERMISSION_FIELD_CACHE_KEY, allRules);
                    //发送通知，权限变更
                    return cacheUtils.convertAndSend(SecurityConstants.Abac.PERMISSION_CHANGE_NOTICE_TOPIC, "syncField");
                }

        );


    }

    @PostConstruct
    public void init() {
        syncPolicies();
        syncFieldPolicies();
    }

}
