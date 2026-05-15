package org.quyq.gwsu.security.apiresource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.cache.utils.lock.DistributedLock;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.collector.ApiEndpointCollector;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.service.PermissionAlterationManager;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiTableModelService;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.api.apiresource.dto.ApiResourceQueryDTO;
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiResourceMapper;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiResourceService;
import org.quyq.gwsu.security.api.vo.ApiResourceVO;
import org.redisson.RedissonShutdownException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 接口资源服务实现
 *
 * @author Quyq
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityApiResourceServiceImpl extends ServiceImpl<SecurityApiResourceMapper, SecurityApiResource>
        implements ISecurityApiResourceService {

    private final CacheUtils cacheUtils;

    private final PermissionAlterationManager permissionAlterationManager;

    private final ISecurityApiTableModelService apiTableModelService;

    @PostConstruct
    public void init() {
        new Thread(new HandleAllServerPermissionRunner(cacheUtils)).start();
    }

    @Override
    public ApiResourceVO getById(Long id) {
        SecurityApiResource entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public ApiResourceVO getByPathAndMethod(String reqPath, String reqMethod) {
        SecurityApiResource entity = getOne(new LambdaQueryWrapper<SecurityApiResource>()
                .eq(SecurityApiResource::getReqPath, reqPath)
                .eq(SecurityApiResource::getReqMethod, reqMethod));
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public List<ApiResourceVO> listByModulePrefix(String modulePrefix) {
        return list(new LambdaQueryWrapper<SecurityApiResource>()
                .eq(SecurityApiResource::getModulePrefix, modulePrefix))
                .stream()
                .map(SecurityApiResource::toVo)
                .toList();
    }

    @Override
    public List<ApiResourceVO> listByTagName(String tagName) {
        return list(new LambdaQueryWrapper<SecurityApiResource>()
                .eq(SecurityApiResource::getTagName, tagName))
                .stream()
                .map(SecurityApiResource::toVo)
                .toList();
    }

    @Override
    public IPage<ApiResourceVO> pageByCondition(ApiResourceQueryDTO query) {
        Page<ApiResourceVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.selectPageVo(page, query);
    }

    @Override
    public Boolean saveOrUpdateBatch(List<SecurityApiResource> entities) {
        return saveOrUpdateBatch(entities, 500);
    }

    @Override
    public Boolean removeByIds(List<Long> ids) {
        return removeBatchByIds(ids);
    }

    @Override
    @Transactional
    @DistributedLock("#applicationName")
    public void handlePermission(String applicationName, ApiEndpointCollector.ApiEndpointWrapper permissions) {
        // 构建新资源列表
        List<SecurityApiResource> newResources = permissions.endpoints().values()
                .stream()
                .flatMap(Collection::stream)
                .map(SecurityApiResource::EndpointInfo2Resource)
                .toList();


        // 查询数据库中该模块的旧资源
        Set<String> modules = permissions.endpoints().keySet();
        List<SecurityApiResource> oldResources = lambdaQuery()
                .in(SecurityApiResource::getModulePrefix, modules)
                .list();

        // 处理表模型绑定数据（在 hasChanged 判断之前，与接口资源共享同一事务和分布式锁）
        apiTableModelService.handleTableModel(applicationName, permissions);

        // 判断是否有变动：数量不同或内容不同
        boolean hasChanged = isResourceChanged(newResources, oldResources);

        if (!hasChanged) {
            return;
        }

        log.info("模块 {} 的API资源有变动，执行更新操作。旧资源数: {}, 新资源数: {}",
                applicationName, oldResources.size(), newResources.size());

        // 删除旧数据
        if (!CollectionUtils.isEmpty(oldResources)) {
            List<String> oldIds = oldResources.stream()
                    .map(SecurityApiResource::getId)
                    .toList();
            removeByIds(oldIds);
        }

        // 插入新数据
        if (!CollectionUtils.isEmpty(newResources)) {
            saveBatch(newResources);
        }
        //变更url权限
        permissionAlterationManager.alterationUrlPermission(AbacPerType.API_RESOURCE, new ExpressionContext());
    }

    /**
     * 判断资源是否有变动
     *
     * @param newResources 新资源列表
     * @param oldResources 旧资源列表
     * @return true-有变动，false-无变动
     */
    private boolean isResourceChanged(List<SecurityApiResource> newResources, List<SecurityApiResource> oldResources) {
        // 数量不同，有变动
        if (newResources.size() != oldResources.size()) {
            return true;
        }

        // 数量相同，比较内容
        // 按 id 排序后逐个比较
        List<SecurityApiResource> sortedNew = newResources.stream()
                .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getId(), b.getId()))
                .toList();

        List<SecurityApiResource> sortedOld = oldResources.stream()
                .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getId(), b.getId()))
                .toList();

        for (int i = 0; i < sortedNew.size(); i++) {
            if (!sortedNew.get(i).equals(sortedOld.get(i))) {
                return true;
            }
        }

        return false;
    }


    private static class HandleAllServerPermissionRunner implements Runnable {

        private final CacheUtils cacheUtils;


        private HandleAllServerPermissionRunner(CacheUtils cacheUtils) {
            this.cacheUtils = cacheUtils;
        }

        private boolean isStop = false;

        @Override
        public void run() {

            ISecurityApiResourceService apiResourceService = SpringUtils.getBean(ISecurityApiResourceService.class);

            while (!isStop) {
                String applicationName = "";
                try {

                    //监听redis指定队列，获取其他服务推送的API资源信息
                    ApiEndpointCollector.ApiEndpointWrapper permissions = cacheUtils.withRebel(() -> cacheUtils.rPop(ApiEndpointCollector.PERMISSION_API_CHANNEL, 0, TimeUnit.SECONDS));


                    if (Objects.isNull(permissions) || CollectionUtils.isEmpty(permissions.endpoints()))
                        continue;

                    applicationName = permissions.applicationName();

                    log.info("接收到 {} 服务的权限信息!", applicationName);

                    apiResourceService.handlePermission(applicationName, permissions);
                } catch (InvalidDataAccessApiUsageException | RedissonShutdownException _) {
                    isStop = true;
                } catch (Exception e) {
                    log.error("处理 {} 服务权限异常", applicationName, e);
                    //如果监听异常，每隔一小时重试
                    try {
                        TimeUnit.HOURS.sleep(1);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            log.info("permission handler stop");
        }

    }

}
