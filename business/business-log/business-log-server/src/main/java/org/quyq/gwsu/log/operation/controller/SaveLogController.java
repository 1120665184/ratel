package org.quyq.gwsu.log.operation.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.api.ILogClientApi;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Quyq
 * @date 2026/5/21
 * @description
 */
@Slf4j
@RestController
@RequestMapping("saveLog")
@Tag(name = "日志记录保存模块", description = "日志记录保存模块")
@RequiredArgsConstructor
public class SaveLogController implements ILogClientApi {

    @Operation(summary = "保存操作日志")
    @PostMapping("operation")
    @Override
    public R<Boolean> saveOperLog(@RequestBody LogOperationVO vo) {
        // TODO 后续实现持久化存储
        log.info("收到操作日志：operId={}, module={}, url={}", vo.getOperId(), vo.getModulePrefix(), vo.getRequestUrl());
        return R.ok(true);
    }
}
