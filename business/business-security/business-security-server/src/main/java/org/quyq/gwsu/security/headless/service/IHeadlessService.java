package org.quyq.gwsu.security.headless.service;


import org.quyq.gwsu.security.headless.domain.HeadlessCallConfig;
import org.quyq.gwsu.security.headless.domain.HeadlessResponse;
import reactor.core.publisher.Flux;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
public interface IHeadlessService {


    Flux<HeadlessResponse> stream(String query, HeadlessCallConfig config);

}
