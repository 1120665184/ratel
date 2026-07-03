package org.quyq.gwsu.kit.job.scheduler.route.strategy;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 一致性HASH路由策略
 */
public class ExecutorRouteConsistentHash extends ExecutorRouter {

    private static final int VIRTUAL_NODE_NUM = 100;

    /**
     * 计算2^32环上的hash值（MD5散列）
     */
    private static long hash(String key) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        md5.update(keyBytes);
        byte[] digest = md5.digest();

        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        return hashCode & 0xffffffffL;
    }

    /**
     * 根据jobId获取地址
     */
    public String hashJob(String jobId, List<String> addressList) {
        // 1、构建hash环
        TreeMap<Long, String> addressRing = new TreeMap<>();
        for (String address : addressList) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                long addressHash = hash("SHARD-" + address + "-NODE-" + i);
                addressRing.put(addressHash, address);
            }
        }

        // 2、生成job-hash
        long jobHash = hash(jobId);

        // 3、路由job节点
        Map.Entry<Long, String> ceilingEntry = addressRing.ceilingEntry(jobHash);
        if (ceilingEntry != null) {
            return ceilingEntry.getValue();
        }

        // 4、默认首节点
        return addressRing.firstEntry().getValue();
    }

    @Override
    public R<String> route(TriggerRequest triggerParam, KitJobGroup jobGroup) {
        List<String> addressList = jobGroup.getRegistryList();
        String address = hashJob(triggerParam.getJobId(), addressList);
        return R.ok(address);
    }

}
