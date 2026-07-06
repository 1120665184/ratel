package org.quyq.gwsu.kit.job.scheduler.route.strategy;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 最不经常使用路由策略（LFU）
 */
public class ExecutorRouteLFU extends ExecutorRouter {

    private static final ConcurrentMap<String, HashMap<String, Integer>> jobLfuMap = new ConcurrentHashMap<>();
    private static long CACHE_VALID_TIME = 0;

    public String route(String jobId, List<String> addressList) {

        // 缓存清除
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLfuMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }

        // lfu项初始化
        HashMap<String, Integer> lfuItemMap = jobLfuMap.get(jobId);
        if (lfuItemMap == null) {
            lfuItemMap = new HashMap<>();
            jobLfuMap.putIfAbsent(jobId, lfuItemMap);
        }

        // 添加新地址
        for (String address : addressList) {
            if (!lfuItemMap.containsKey(address) || lfuItemMap.get(address) > 1000000) {
                lfuItemMap.put(address, new Random().nextInt(addressList.size()));
            }
        }
        // 移除旧地址
        List<String> delKeys = new ArrayList<>();
        for (String existKey : lfuItemMap.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        if (!delKeys.isEmpty()) {
            for (String delKey : delKeys) {
                lfuItemMap.remove(delKey);
            }
        }

        // 加载使用次数最少的地址
        List<Map.Entry<String, Integer>> lfuItemList = new ArrayList<>(lfuItemMap.entrySet());
        lfuItemList.sort(Map.Entry.comparingByValue());

        Map.Entry<String, Integer> addressItem = lfuItemList.get(0);
        addressItem.setValue(addressItem.getValue() + 1);

        return addressItem.getKey();
    }

    @Override
    public R<String> route(TriggerRequest triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return R.ok(address);
    }

}
