package org.quyq.gwsu.common.core.utils.filter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/1
 * @description
 */
@Slf4j
public class ProcessorChain {

    private final List<RequestResponseProcessor> processors;

    private final List<RequestResponseProcessor> reversedProcessors;

    public ProcessorChain(List<RequestResponseProcessor> processors) {
        this.processors = CollectionUtils.isEmpty(processors) ? Collections.emptyList() : processors.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();
        // 构造逆序列表，供 postHandle 使用
        List<RequestResponseProcessor> reversed = new ArrayList<>(this.processors);
        Collections.reverse(reversed);
        this.reversedProcessors = List.copyOf(reversed);
        log.info("Loaded {} request processors: {}", this.processors.size(),
                this.processors.stream().map(p -> p.getClass().getSimpleName()).toList());
    }

    /**
     * 判断是否有处理器需要响应体。
     *
     * @param context 请求上下文
     * @return true 表示至少有一个处理器需要响应体
     */
    public boolean isAnyNeedsResponseBody(RequestResponseContext context) {
        return processors.stream()
                .anyMatch(processor -> processor.needsResponseBody(context));
    }

    public Mono<Boolean> executePreHandlers(RequestResponseContext context) {
        return Flux.fromIterable(processors)
                .concatMap(processor -> processor.preHandle(context)
                        .doOnNext(success -> {
                            if (!success) {
                                log.debug("Processor {} interrupted the chain", processor.getClass().getSimpleName());
                            }
                        }))
                .takeWhile(success -> success)
                .then(Mono.just(true))
                .defaultIfEmpty(false);
    }

    public Mono<Void> executePostHandlers(RequestResponseContext context) {
        return Flux.fromIterable(reversedProcessors)
                .concatMap(processor -> processor.postHandle(context)
                        .doOnError(e -> log.error("Error in postHandle of {}",
                                processor.getClass().getSimpleName(), e))
                        .onErrorResume(e -> Mono.empty()))
                .then();
    }

}
