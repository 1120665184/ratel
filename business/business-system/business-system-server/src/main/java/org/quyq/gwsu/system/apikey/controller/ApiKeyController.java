package org.quyq.gwsu.system.apikey.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.system.api.apikey.dto.ApiKeyCreateDTO;
import org.quyq.gwsu.system.api.apikey.dto.ApiKeyQueryDTO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyCreateResultVO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyDetailVO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyVO;
import org.quyq.gwsu.system.apikey.service.ISysApiKeyService;
import org.springframework.web.bind.annotation.*;

/**
 * API_KEY 管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("api-key")
@Tag(name = "API_KEY 管理")
@RequiredArgsConstructor
@LoginAllowAccess
public class ApiKeyController {

    private final ISysApiKeyService apiKeyService;

    @PostMapping("create")
    @Operation(summary = "创建当前用户 API_KEY")
    public R<ApiKeyCreateResultVO> create(@RequestBody ApiKeyCreateDTO dto) {
        return R.ok(apiKeyService.createCurrentUserApiKey(dto));
    }

    @PostMapping("page")
    @Operation(summary = "分页查询当前用户 API_KEY")
    public R<IPage<ApiKeyVO>> page(@RequestBody ApiKeyQueryDTO query) {
        return R.ok(apiKeyService.pageCurrentUserApiKeys(query));
    }

    @GetMapping("{id}")
    @Operation(summary = "查询当前用户 API_KEY 详情")
    public R<ApiKeyDetailVO> detail(@PathVariable String id) {
        return R.ok(apiKeyService.getCurrentUserApiKeyDetail(id));
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除当前用户 API_KEY")
    public R<Boolean> remove(@PathVariable String id) {
        return R.ok(apiKeyService.removeCurrentUserApiKey(id));
    }
}
