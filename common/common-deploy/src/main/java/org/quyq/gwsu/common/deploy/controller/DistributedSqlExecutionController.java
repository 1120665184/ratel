package org.quyq.gwsu.common.deploy.controller;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.deploy.dto.SQLQueryDTO;
import org.quyq.gwsu.common.security.domain.vo.SqlQueryVO;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description sql执行控制器
 */
@ResponseBody
@RestController
@RequiredArgsConstructor
public class DistributedSqlExecutionController {

    private final ISQLExecutionService sqlExecutionService;


    /**
     * 执行sql
     * @param dto
     * @return
     */
    @PostMapping(CoreConstants.EndPoint.ENDPOINT_DB_EXECUTION)
    public R<SqlQueryVO> query(@RequestBody SQLQueryDTO dto) {
        return R.ok(sqlExecutionService.query(dto.datasource(), dto.sql(), dto.params()));
    }

}
