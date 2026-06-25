package org.quyq.gwsu.headless.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.headless.api.HeadlessClientApi;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.vo.HeadlessResponse;
import org.quyq.gwsu.headless.domain.HeadlessCallConfig;
import org.quyq.gwsu.headless.service.IHeadlessService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author Quyq
 * @date 2026/6/25
 * @description
 */
@RestController
@RequestMapping("headless")
@Tag(name = "无头智能体模块")
@RequiredArgsConstructor
public class HeadlessController implements HeadlessClientApi {

    private final IHeadlessService headlessService;

    @Override
    @PostMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(description = "无头智能体调用")
    public Flux<HeadlessResponse> stream(@RequestBody HeadlessDTO form) {

        return headlessService.stream(form.message().getTextContent(), HeadlessCallConfig.builder()
                .userId(form.userId())
                .threadId(form.threadId())
                .build());

    }

}
