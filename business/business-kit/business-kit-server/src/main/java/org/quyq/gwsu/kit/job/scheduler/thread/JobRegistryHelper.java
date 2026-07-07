package org.quyq.gwsu.kit.job.scheduler.thread;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.common.job.openapi.admin.dto.RegistryRequest;
import org.quyq.gwsu.kit.job.domain.KitJobRegistry;
import org.quyq.gwsu.kit.job.service.IKitJobRegistryService;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 执行器注册助手
 * <p>
 * 按 handler 聚合地址列表，维护 handler2RegistryCache 内存缓存。
 * 冲突检测：同一个 handler 如果有多个 appname 注册，只取第一个，其余标记冲突并暂停调度。
 */
public class JobRegistryHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    // 注册/注销线程池
    private ThreadPoolExecutor registryOrRemoveThreadPool = null;

    // 注册监控调度器
    private ScheduledExecutorService registryMonitorScheduler;

    // handler注册缓存：handlerName -> HandlerRegistryInfo
    private volatile Map<String, HandlerRegistryInfo> handler2RegistryCache = new ConcurrentHashMap<>();

    // 冲突handler集合：同一handler被不同appname注册
    private volatile Set<String> conflictHandlers = Collections.emptySet();

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

        // 2、注册监控线程
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
            List<String> ids = JobAdminBootstrap.getInstance().getKitJobRegistryService()
                    .findDead(JobConst.REGISTRY_BEAT_INTERVAL * 3, LocalDateTime.now());
            if (ids != null && !ids.isEmpty()) {
                JobAdminBootstrap.getInstance().getKitJobRegistryService().removeBatchByIds(ids);
            }

            // b、获取有效注册列表，按 handler 聚合地址
            List<KitJobRegistry> list = JobAdminBootstrap.getInstance().getKitJobRegistryService()
                    .findAll(JobConst.REGISTRY_BEAT_INTERVAL * 3, LocalDateTime.now());

            // handler -> { appname -> addresses }
            Map<String, Map<String, List<String>>> handlerAppnameAddressMap = new HashMap<>();
            if (list != null) {
                for (KitJobRegistry item : list) {
                    String appname = item.getRegistryGroup();
                    String handlerName = item.getRegistryKey();
                    String address = item.getRegistryValue();

                    handlerAppnameAddressMap
                            .computeIfAbsent(handlerName, k -> new HashMap<>())
                            .computeIfAbsent(appname, k -> new ArrayList<>())
                            .add(address);
                }
            }

            // c、构建缓存，检测冲突
            Map<String, HandlerRegistryInfo> newCache = new ConcurrentHashMap<>();
            Set<String> newConflictHandlers = new HashSet<>();

            for (Map.Entry<String, Map<String, List<String>>> entry : handlerAppnameAddressMap.entrySet()) {
                String handlerName = entry.getKey();
                Map<String, List<String>> appnameMap = entry.getValue();

                if (appnameMap.size() > 1) {
                    // 冲突：同一 handler 被多个 appname 注册
                    newConflictHandlers.add(handlerName);
                    // 取第一个 appname 的地址列表（按字母序保证稳定性）
                    String firstAppname = appnameMap.keySet().stream().sorted().findFirst().orElse(null);
                    List<String> addresses = appnameMap.get(firstAppname);
                    // 去重
                    List<String> uniqueAddresses = addresses.stream().distinct().sorted().toList();
                    newCache.put(handlerName, new HandlerRegistryInfo(handlerName, firstAppname, uniqueAddresses));

                    // 输出被丢弃的handler详细信息
                    List<String> discardedAppnames = appnameMap.keySet().stream()
                            .sorted()
                            .filter(a -> !a.equals(firstAppname))
                            .toList();
                    for (String discardedAppname : discardedAppnames) {
                        List<String> discardedAddresses = appnameMap.get(discardedAppname).stream().distinct().sorted().toList();
                        logger.warn(">>>>>>>>>>> kit-job, handler冲突丢弃! handler:{}, 丢弃appname:{}, 丢弃地址:{}, 保留appname:{}",
                                handlerName, discardedAppname, discardedAddresses, firstAppname);
                    }
                    logger.warn(">>>>>>>>>>> kit-job, handler冲突! handler:{}, 多个appname:{}, 保留第一个:{}",
                            handlerName, appnameMap.keySet(), firstAppname);
                } else {
                    Map.Entry<String, List<String>> single = appnameMap.entrySet().iterator().next();
                    String appname = single.getKey();
                    List<String> addresses = single.getValue();
                    List<String> uniqueAddresses = addresses.stream().distinct().sorted().toList();
                    newCache.put(handlerName, new HandlerRegistryInfo(handlerName, appname, uniqueAddresses));
                }
            }

            // d、刷新缓存
            handler2RegistryCache = newCache;
            conflictHandlers = newConflictHandlers;

            logger.debug(">>>>>>>>>>> kit-job, JobRegistryHelper, 刷新HandlerRegistryCache成功, handlerCount:{}, conflictCount:{}", newCache.size(), newConflictHandlers.size());

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

    // ---------------------- 注册/注销 ----------------------

    /**
     * 注册
     * <p>
     * 注册前检测冲突：如果已存在不同appname且handler同名的注册，则拒绝注册。
     */
    public R<String> registry(RegistryRequest registryParam) {

        // 参数校验
        if (registryParam.getRegistryGroup() == null || registryParam.getRegistryGroup().trim().isEmpty()
                || registryParam.getRegistryKey() == null || registryParam.getRegistryKey().trim().isEmpty()
                || registryParam.getRegistryValue() == null || registryParam.getRegistryValue().trim().isEmpty()) {
            return R.fail("参数不合法");
        }

        // 冲突检测：同一handler是否已被不同appname注册
        String conflictAppname = JobAdminBootstrap.getInstance().getKitJobRegistryService()
                .findConflictAppname(registryParam.getRegistryGroup(), registryParam.getRegistryKey());
        if (conflictAppname != null) {
            logger.warn(">>>>>>>>>>> kit-job, handler注册拒绝! handler:{}, 当前appname:{}, 冲突appname:{}, 同名handler不允许跨appname注册",
                    registryParam.getRegistryKey(), registryParam.getRegistryGroup(), conflictAppname);
            return R.fail("handler冲突: handler[" + registryParam.getRegistryKey()
                    + "]已被appname[" + conflictAppname + "]注册，不允许不同appname注册同名handler");
        }

        // 异步执行
        registryOrRemoveThreadPool.execute(() -> {
            JobAdminBootstrap.getInstance().getKitJobRegistryService()
                    .registrySaveOrUpdate(IdWorker.getIdStr(), registryParam.getRegistryGroup(), registryParam.getRegistryKey(),
                            registryParam.getRegistryValue(), LocalDateTime.now());
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
            JobAdminBootstrap.getInstance().getKitJobRegistryService()
                    .remove(new LambdaQueryWrapper<KitJobRegistry>()
                            .eq(KitJobRegistry::getRegistryGroup, registryParam.getRegistryGroup())
                            .eq(KitJobRegistry::getRegistryKey, registryParam.getRegistryKey())
                            .eq(KitJobRegistry::getRegistryValue, registryParam.getRegistryValue()));
        });

        return R.ok();
    }

    // ---------------------- 缓存查询 ----------------------

    /**
     * 根据handler名称获取注册信息
     */
    public HandlerRegistryInfo loadByHandlerName(String handlerName) {
        return handler2RegistryCache.get(handlerName);
    }

    /**
     * 根据handler名称获取在线地址列表
     */
    public List<String> getAddressList(String handlerName) {
        HandlerRegistryInfo info = handler2RegistryCache.get(handlerName);
        return info != null ? info.addresses() : Collections.emptyList();
    }

    /**
     * 判断handler是否冲突
     */
    public boolean isConflict(String handlerName) {
        return conflictHandlers.contains(handlerName);
    }

    /**
     * 获取所有handler注册信息
     */
    public Map<String, HandlerRegistryInfo> getAllHandlerRegistry() {
        return handler2RegistryCache;
    }

    /**
     * 获取冲突handler集合
     */
    public Set<String> getConflictHandlers() {
        return conflictHandlers;
    }

}
