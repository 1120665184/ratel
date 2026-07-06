package org.quyq.gwsu.kit.job.scheduler.route.strategy;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 最近最久未使用路由策略（LRU）
 */
public class ExecutorRouteLRU extends ExecutorRouter {

    private static final ConcurrentMap<String, LinkedHashMap<String, String>> jobLRUMap = new ConcurrentHashMap<>();
    private static long CACHE_VALID_TIME = 0;

    public String route(String jobId, List<String> addressList) {

        // 缓存清除
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLRUMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }

        // 初始化LRU
        LinkedHashMap<String, String> lruItem = jobLRUMap.get(jobId);
        if (lruItem == null) {
            lruItem = new LinkedHashMap<>(16, 0.75f, true);
            jobLRUMap.putIfAbsent(jobId, lruItem);
        }

        // 添加新地址
        for (String address : addressList) {
            if (!lruItem.containsKey(address)) {
                lruItem.put(address, address);
            }
        }
        // 移除旧地址
        List<String> delKeys = new ArrayList<>();
        for (String existKey : lruItem.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        if (!delKeys.isEmpty()) {
            for (String delKey : delKeys) {
                lruItem.remove(delKey);
            }
        }

        // 获取最久未使用的地址
        String eldestKey = lruItem.entrySet().iterator().next().getKey();
        return lruItem.get(eldestKey);
    }

    @Override
    public R<String> route(TriggerRequest triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return R.ok(address);
    }

}
