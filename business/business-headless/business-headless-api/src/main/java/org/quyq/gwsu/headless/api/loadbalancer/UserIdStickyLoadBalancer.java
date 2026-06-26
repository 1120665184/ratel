package org.quyq.gwsu.headless.api.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于 userId 的会话亲和负载均衡器
 * <p>
 * 同一 userId 始终路由到同一个服务实例（一致性 hash），
 * 确保无头浏览器缓存的 Session 能被后续请求命中。
 * <p>
 * 当目标实例不可用时，自动清除亲和映射并重新选择。
 * <p>
 * userId 从请求 URL 的查询参数 {@code ?userId=xxx} 中提取，
 * 由 {@link org.quyq.gwsu.common.api.loadbalancer.ServiceInstanceLoadBalancerFilterFunction}
 * 将 URL 信息封装到 {@link RequestDataContext} 中传入。
 * <p>
 * 构造签名符合 {@link ReactorServiceInstanceLoadBalancer} 规范，
 * 可通过 {@link org.quyq.gwsu.common.api.annotation.ApiClient#loadBalancer()} 直接指定。
 *
 * @author Quyq
 */
@Slf4j
public class UserIdStickyLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final String serviceId;

    /**
     * userId → 实例索引 的亲和映射
     */
    private final ConcurrentHashMap<String, Integer> affinityMap = new ConcurrentHashMap<>();

    public UserIdStickyLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                    String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        String userId = extractUserId(request);
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }

        return supplier.get().next().map(instances -> selectInstance(instances, userId));
    }

    /**
     * 兼容无参 choose() 调用（降级为空响应）
     */
    @Override
    public Mono<Response<ServiceInstance>> choose() {
        return Mono.just(new EmptyResponse());
    }

    /**
     * 选择服务实例
     * <p>
     * 1. 有 userId 亲和映射 → 优先返回映射的实例
     * 2. 映射的实例不在可用列表中 → 清除映射，重新 hash 选择
     * 3. 无 userId → 轮询选择
     */
    private Response<ServiceInstance> selectInstance(List<ServiceInstance> instances, String userId) {
        if (instances == null || instances.isEmpty()) {
            return new EmptyResponse();
        }

        // 无 userId，简单轮询
        if (userId == null || userId.isEmpty()) {
            int index = (int) (System.currentTimeMillis() % instances.size());
            return new DefaultResponse(instances.get(index));
        }

        // 1. 检查亲和映射
        Integer affinityIndex = affinityMap.get(userId);
        if (affinityIndex != null && affinityIndex < instances.size()) {
            ServiceInstance instance = instances.get(affinityIndex);
            log.debug("会话亲和命中: userId={}, instance={}:{}", userId, instance.getHost(), instance.getPort());
            return new DefaultResponse(instance);
        }

        // 2. 亲和映射失效（实例下线），清除并重新选择
        if (affinityIndex != null) {
            affinityMap.remove(userId);
            log.debug("亲和实例已下线，重新选择: userId={}", userId);
        }

        // 3. 一致性 hash 选择新实例
        int index = Math.abs(userId.hashCode()) % instances.size();
        affinityMap.put(userId, index);

        ServiceInstance instance = instances.get(index);
        log.info("会话亲和绑定: userId={}, instance={}:{}", userId, instance.getHost(), instance.getPort());
        return new DefaultResponse(instance);
    }

    /**
     * 从 Request 的 context 中提取 userId
     * <p>
     * ServiceInstanceLoadBalancerFilterFunction 会将请求 URL 封装到 RequestDataContext 中，
     * 此方法从 URL 查询参数中解析 userId（hint 是 Spring Cloud 的区域路由约定，不用于传递业务参数）
     */
    private String extractUserId(Request request) {
        if (request == null || request.getContext() == null) {
            return null;
        }

        Object context = request.getContext();

        if (context instanceof RequestDataContext rdc) {
            // 直接从 URL 查询参数中解析 userId
            RequestData requestData = rdc.getClientRequest();
            if (requestData != null) {
                return extractUserIdFromQuery(requestData.getUrl());
            }
        }

        return null;
    }

    /**
     * 从 URI 查询参数中提取 userId
     */
    private String extractUserIdFromQuery(URI uri) {
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            return null;
        }

        Map<String, String> params = Arrays.stream(query.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

        return params.get("userId");
    }
}
