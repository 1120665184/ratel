package org.quyq.gwsu.headless.api.factory;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.headless.api.HeadlessClientApi;
import reactor.core.publisher.Flux;

/**
 * @author Quyq
 * @date 2026/6/25
 * @description
 */
@Slf4j
public class HeadlessClientApiFactory implements FallbackFactory<HeadlessClientApi> {
    @Override
    public HeadlessClientApi create(Throwable cause) {
        log.error("", cause);
        return form -> Flux.error(cause);
    }
}
