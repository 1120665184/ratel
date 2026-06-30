package org.quyq.gwsu.headless.service;


import org.quyq.gwsu.headless.api.dto.UserMsg;
import org.quyq.gwsu.headless.api.vo.HeadlessResponse;
import org.quyq.gwsu.headless.domain.HeadlessCallConfig;
import reactor.core.publisher.Flux;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
public interface IHeadlessService {


    Flux<HeadlessResponse> stream(UserMsg msg, HeadlessCallConfig config);

}
