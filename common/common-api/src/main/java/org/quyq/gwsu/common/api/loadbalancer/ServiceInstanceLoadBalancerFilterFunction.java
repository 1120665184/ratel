package org.quyq.gwsu.common.api.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 基于 {@link ReactorServiceInstanceLoadBalancer} 的 WebClient 负载均衡过滤器。
 * <p>
 * 使用指定的负载均衡策略选择服务实例，并将请求 URI 中的逻辑服务名替换为实际地址。
 * 用于 {@link org.quyq.gwsu.common.api.annotation.ApiClient#loadBalancer()} 指定的自定义策略场景。
 * <p>
 * 会将请求的 HTTP 方法和 URL 封装为 {@link RequestDataContext} 传入 LoadBalancer，
 * 使自定义 LoadBalancer 能从 Request 中获取 URL 信息（如查询参数）进行路由决策。
 *
 * @author Quyq
 * @date 2026/6/25
 */
public class ServiceInstanceLoadBalancerFilterFunction implements ExchangeFilterFunction {

    private final ReactorServiceInstanceLoadBalancer loadBalancer;

    public ServiceInstanceLoadBalancerFilterFunction(ReactorServiceInstanceLoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // 构建 LoadBalancer Request，携带请求的 HTTP 方法和 URL 信息
        RequestData requestData = new RequestData(request);
        RequestDataContext context = new RequestDataContext(requestData, "default");
        DefaultRequest<RequestDataContext> lbRequest = new DefaultRequest<>(context);

        return loadBalancer.choose(lbRequest)
                .flatMap(response -> {
                    ServiceInstance instance = response.getServer();
                    if (instance == null) {
                        return next.exchange(request);
                    }
                    URI reconstructedUri = reconstructUri(request.url(), instance);
                    ClientRequest newRequest = ClientRequest.from(request).url(reconstructedUri).build();
                    return next.exchange(newRequest);
                })
                .switchIfEmpty(Mono.defer(() -> next.exchange(request)));
    }

    /**
     * 根据服务实例信息重构 URI
     * 将逻辑服务名（如 http://gwsu-security）替换为实际地址（如 http://192.168.1.10:8081）
     */
    private URI reconstructUri(URI originalUri, ServiceInstance instance) {
        String scheme = instance.isSecure() ? "https" : "http";
        try {
            return new URI(scheme, null, instance.getHost(), instance.getPort(),
                    originalUri.getPath(), originalUri.getQuery(), originalUri.getFragment());
        } catch (Exception e) {
            throw new RuntimeException("负载均衡 URI 重构失败: " + originalUri, e);
        }
    }
}
