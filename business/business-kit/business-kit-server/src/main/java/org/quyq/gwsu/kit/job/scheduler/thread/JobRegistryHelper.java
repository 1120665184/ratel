package org.quyq.gwsu.kit.job.scheduler.thread;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.common.job.constant.RegistTypeEnum;
import org.quyq.gwsu.common.job.openapi.admin.dto.RegistryRequest;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;
import org.quyq.gwsu.kit.job.domain.KitJobRegistry;
import org.quyq.gwsu.kit.job.mapper.KitJobGroupMapper;
import org.quyq.gwsu.kit.job.mapper.KitJobRegistryMapper;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 执行器注册助手
 */
public class JobRegistryHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    // 注册/注销线程池
    private ThreadPoolExecutor registryOrRemoveThreadPool = null;

    // 注册监控调度器
    private ScheduledExecutorService registryMonitorScheduler;

    // 任务组缓存
    private volatile Map<String, KitJobGroup> appname2GroupCache = new ConcurrentHashMap<>();
    private volatile Map<String, KitJobGroup> id2GroupCache = new ConcurrentHashMap<>();

    /**
     * 启动
     */
    public void start() {

        // 1、注册/注销线程池
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "kit-job, admin JobRegistryHelper-registryOrRemoveThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    logger.warn(">>>>>>>>>>> kit-job, 注册或注销操作过快，触发拒绝策略（直接执行）。");
                });

        // 2、注册监控线程（使用ScheduledExecutorService替代CyclicThread）
        registryMonitorScheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "kit-job, admin JobRegistryHelper-registryMonitorThread"));
        registryMonitorScheduler.scheduleAtFixedRate(this::registryMonitorTask,
                JobConst.REGISTRY_BEAT_INTERVAL,
                JobConst.REGISTRY_BEAT_INTERVAL,
                TimeUnit.SECONDS);
    }

    /**
     * 注册监控任务
     */
    private void registryMonitorTask() {
        try {
            // a、移除失效地址
            List<String> ids = JobAdminBootstrap.getInstance().getKitJobRegistryMapper().findDead(JobConst.REGISTRY_BEAT_INTERVAL * 3, LocalDateTime.now());
            if (ids != null && !ids.isEmpty()) {
                JobAdminBootstrap.getInstance().getKitJobRegistryMapper().deleteBatchIds(ids);
            }

            // b、获取在线地址（appname : List<address>）
            HashMap<String, List<String>> appnameAddressMap = new HashMap<>();
            List<KitJobRegistry> list = JobAdminBootstrap.getInstance().getKitJobRegistryMapper().findAll(JobConst.REGISTRY_BEAT_INTERVAL * 3, LocalDateTime.now());
            if (list != null) {
                for (KitJobRegistry item : list) {
                    if (RegistTypeEnum.EXECUTOR.name().equals(item.getRegistryGroup())) {
                        String appname = item.getRegistryKey();
                        List<String> registryList = appnameAddressMap.computeIfAbsent(appname, k -> new ArrayList<>());

                        if (!registryList.contains(item.getRegistryValue())) {
                            registryList.add(item.getRegistryValue());
                        }
                    }
                }
            }

            // c、自动注册：为没有对应执行器组的appname自动创建组
            if (!appnameAddressMap.isEmpty()) {
                List<KitJobGroup> existingGroups = JobAdminBootstrap.getInstance().getKitJobGroupMapper()
                        .selectList(new LambdaQueryWrapper<KitJobGroup>().eq(KitJobGroup::getAddressType, 0));
                Set<String> existingAppnames = existingGroups != null
                        ? existingGroups.stream().map(KitJobGroup::getAppname).collect(java.util.stream.Collectors.toSet())
                        : Collections.emptySet();

                for (String appname : appnameAddressMap.keySet()) {
                    if (!existingAppnames.contains(appname)) {
                        KitJobGroup newGroup = new KitJobGroup();
                        newGroup.setAppname(appname);
                        newGroup.setName(appname);
                        newGroup.setAddressType(0);
                        JobAdminBootstrap.getInstance().getKitJobGroupMapper().insert(newGroup);
                        logger.info(">>>>>>>>>>> kit-job, 自动注册执行器组, appname:{}", appname);
                    }
                }
            }

            // d、刷新自动注册的执行器组地址
            List<KitJobGroup> groupList = JobAdminBootstrap.getInstance().getKitJobGroupMapper()
                    .selectList(new LambdaQueryWrapper<KitJobGroup>().eq(KitJobGroup::getAddressType, 0));
            if (groupList != null && !groupList.isEmpty()) {
                for (KitJobGroup group : groupList) {
                    List<String> registryList = appnameAddressMap.get(group.getAppname());
                    String addressListStr = null;
                    if (registryList != null && !registryList.isEmpty()) {
                        Collections.sort(registryList);
                        addressListStr = String.join(",", registryList);
                    }
                    group.setAddressList(addressListStr);

                    JobAdminBootstrap.getInstance().getKitJobGroupMapper().updateById(group);
                }
            }

            // 2.2、刷新本地缓存
            List<KitJobGroup> jobGroupList = JobAdminBootstrap.getInstance().getKitJobGroupMapper().selectList(null);
            Map<String, KitJobGroup> appname2GroupCacheNew = new ConcurrentHashMap<>();
            Map<String, KitJobGroup> id2GroupCacheNew = new ConcurrentHashMap<>();
            if (jobGroupList != null && !jobGroupList.isEmpty()) {
                for (KitJobGroup group : jobGroupList) {
                    appname2GroupCacheNew.put(group.getAppname(), group);
                    id2GroupCacheNew.put(group.getId(), group);
                }
            }
            if (!toJson(appname2GroupCacheNew).equals(toJson(appname2GroupCache))) {
                appname2GroupCache = appname2GroupCacheNew;
                id2GroupCache = id2GroupCacheNew;
                logger.info(">>>>>>>>>>> kit-job, JobRegistryHelper, 检测到变化并刷新JobGroupCache成功");
            }
            logger.debug(">>>>>>>>>>> kit-job, JobRegistryHelper, 刷新JobGroupCache成功");

        } catch (Exception e) {
            logger.error(">>>>>>>>>>> kit-job, JobRegistryHelper#registryMonitorTask error:{}", e.getMessage(), e);
        }
    }

    /**
     * 停止
     */
    public void stop() {
        registryOrRemoveThreadPool.shutdownNow();
        registryMonitorScheduler.shutdownNow();
    }

    // ---------------------- 工具方法 ----------------------

    /**
     * 注册
     */
    public R<String> registry(RegistryRequest registryParam) {

        // 参数校验
        if (registryParam.getRegistryGroup() == null || registryParam.getRegistryGroup().trim().isEmpty()
                || registryParam.getRegistryKey() == null || registryParam.getRegistryKey().trim().isEmpty()
                || registryParam.getRegistryValue() == null || registryParam.getRegistryValue().trim().isEmpty()) {
            return R.fail("参数不合法");
        }

        // 异步执行
        registryOrRemoveThreadPool.execute(() -> {
            int ret = JobAdminBootstrap.getInstance().getKitJobRegistryMapper()
                    .registrySaveOrUpdate(IdWorker.getIdStr(), registryParam.getRegistryGroup(), registryParam.getRegistryKey(),
                            registryParam.getRegistryValue(), LocalDateTime.now());
            if (ret == 1) {
                freshGroupRegistryInfo(registryParam);
            }
        });

        return R.ok();
    }

    /**
     * 注销注册
     */
    public R<String> registryRemove(RegistryRequest registryParam) {

        // 参数校验
        if (registryParam.getRegistryGroup() == null || registryParam.getRegistryGroup().trim().isEmpty()
                || registryParam.getRegistryKey() == null || registryParam.getRegistryKey().trim().isEmpty()
                || registryParam.getRegistryValue() == null || registryParam.getRegistryValue().trim().isEmpty()) {
            return R.fail("参数不合法");
        }

        // 异步执行
        registryOrRemoveThreadPool.execute(() -> {
            int ret = JobAdminBootstrap.getInstance().getKitJobRegistryMapper()
                    .delete(new LambdaQueryWrapper<KitJobRegistry>()
                            .eq(KitJobRegistry::getRegistryGroup, registryParam.getRegistryGroup())
                            .eq(KitJobRegistry::getRegistryKey, registryParam.getRegistryKey())
                            .eq(KitJobRegistry::getRegistryValue, registryParam.getRegistryValue()));
            if (ret > 0) {
                freshGroupRegistryInfo(registryParam);
            }
        });

        return R.ok();
    }

    private void freshGroupRegistryInfo(RegistryRequest registryParam) {
        // 预留，防止影响核心表
    }

    // ---------------------- 缓存 ----------------------

    /**
     * 根据ID加载执行器组
     */
    public KitJobGroup load(String jobGroup) {
        return id2GroupCache.get(jobGroup);
    }

    /**
     * 根据appname加载执行器组
     */
    public KitJobGroup loadByAppName(String appname) {
        return appname2GroupCache.get(appname);
    }

    private String toJson(Object obj) {
        try {
            return JobAdminBootstrap.getInstance().getObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

}
