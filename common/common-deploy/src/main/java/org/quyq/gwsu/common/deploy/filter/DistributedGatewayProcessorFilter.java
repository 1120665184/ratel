package org.quyq.gwsu.common.deploy.filter;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.filter.ProcessorChain;
import org.quyq.gwsu.common.core.utils.filter.RequestResponseContext;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * @author Quyq
 * @date 2026/4/1
 * @description 分布式版网关处理过滤器处理逻辑
 */
@RequiredArgsConstructor
@Slf4j
public class DistributedGatewayProcessorFilter implements GlobalFilter, Ordered {

    private final ProcessorChain processorChain;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        RequestResponseContext context = buildContext(request);

        return processorChain.executePreHandlers(context)
                .flatMap(preSuccess -> {
                    if (!preSuccess) {
                        applyResponse(context, exchange.getResponse());
                        return exchange.getResponse().setComplete();
                    }

                    // 从context中获取可能被preHandler修改过的请求头，重新赋值到exchange
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .headers(httpHeaders -> {
                                httpHeaders.clear();
                                context.getHeaders().forEach(httpHeaders::put);
                            })
                            .build();
                    ServerWebExchange contextExchange = exchange.mutate()
                            .request(mutatedRequest)
                            .build();

                    if (processorChain.isAnyNeedsResponseBody(context)) {
                        return filterWithBodyCapture(contextExchange, chain, context);
                    } else {
                        return chain.filter(contextExchange)
                                .then(processorChain.executePostHandlers(context))
                                .then(Mono.fromRunnable(() -> applyResponse(context, exchange.getResponse())));
                    }
                });
    }

    /**
     * 需要捕获响应体时的处理逻辑
     */
    private Mono<Void> filterWithBodyCapture(ServerWebExchange exchange, GatewayFilterChain chain, RequestResponseContext context) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ResponseBodyCaptor captor = new ResponseBodyCaptor(originalResponse, bufferFactory, context);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .response(captor)
                .build();

        // 1. 执行业务链（writeWith 只捕获数据，不写出）
        // 2. 执行 postHandle（可能修改 modifiedResponseBody）
        // 3. 根据情况写出最终响应
        return chain.filter(mutatedExchange)
                .then(processorChain.executePostHandlers(context))
                .then(Mono.defer(() -> {
                    applyResponse(context, originalResponse);
                    // 写出最终响应
                    return captor.writeFinalResponse(originalResponse);
                }));
    }


    private void applyResponse(RequestResponseContext context, ServerHttpResponse response) {
        if (!response.isCommitted()) {
            response.setStatusCode(HttpStatus.valueOf(context.getHttpStatus()));
            context.getResponseHeaders().forEach((name, values) ->
                    response.getHeaders().put(name, values));
        }
    }

    private RequestResponseContext buildContext(ServerHttpRequest request) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        request.getHeaders().forEach((k , v) -> headers.put(k.toLowerCase(Locale.ROOT) , v));

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        URI uri = request.getURI();
        if (uri.getQuery() != null) {
            String[] pairs = uri.getQuery().split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    queryParams.add(pair.substring(0, idx), pair.substring(idx + 1));
                } else {
                    queryParams.add(pair, "");
                }
            }
        }

        return new RequestResponseContext(
                uri.getPath(),
                request.getMethod().name(),
                headers,
                queryParams
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 响应体捕获器
     * 只负责捕获响应数据，不立即写出，等待外部调用 writeFinalResponse
     */
    private static class ResponseBodyCaptor extends ServerHttpResponseDecorator {
        private final DataBufferFactory bufferFactory;
        private final RequestResponseContext context;
        private byte[] capturedBytes;

        public ResponseBodyCaptor(ServerHttpResponse delegate, DataBufferFactory bufferFactory, RequestResponseContext context) {
            super(delegate);
            this.bufferFactory = bufferFactory;
            this.context = context;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            // 只捕获数据，不写出
            return Flux.from(body)
                    .collectList()
                    .flatMap(buffers -> {
                        capturedBytes = copyBuffersToBytes(buffers);
                        String responseBody = new String(capturedBytes, StandardCharsets.UTF_8);
                        context.setOriginalResponseBody(responseBody);
                        return Mono.empty();
                    });
        }

        /**
         * 写出最终响应
         */
        public Mono<Void> writeFinalResponse(ServerHttpResponse response) {
            if (capturedBytes == null) {
                return Mono.empty();
            }

            DataBuffer buffer;
            if (context.getModifiedResponseBody() != null) {
                // 使用修改后的响应体
                String modifiedBody = context.getModifiedResponseBody().toString();
                buffer = bufferFactory.wrap(modifiedBody.getBytes(StandardCharsets.UTF_8));
            } else {
                // 使用原始响应体
                buffer = bufferFactory.wrap(capturedBytes);
            }

            return response.writeWith(Mono.just(buffer));
        }

        /**
         * 复制所有 buffer 数据到新的字节数组，并释放原始 buffer
         */
        private byte[] copyBuffersToBytes(List<? extends DataBuffer> buffers) {
            if (buffers.isEmpty()) {
                return new byte[0];
            }

            int totalSize = 0;
            for (DataBuffer buffer : buffers) {
                totalSize += buffer.readableByteCount();
            }

            byte[] result = new byte[totalSize];
            int offset = 0;
            for (DataBuffer buffer : buffers) {
                int length = buffer.readableByteCount();
                buffer.read(result, offset, length);
                offset += length;
                DataBufferUtils.release(buffer);
            }

            return result;
        }
    }

}
