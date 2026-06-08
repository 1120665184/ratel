package org.quyq.gwsu.security.tablemodel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.api.tablemodel.dto.BusinessFunctionQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.tablemodel.domain.SecurityBusinessFunction;
import org.quyq.gwsu.security.tablemodel.domain.SecurityBusinessFunctionTable;
import org.quyq.gwsu.security.tablemodel.service.ISecurityBusinessFunctionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "业务功能配置")
@RestController
@RequestMapping("business-function")
@TableModelPermission({SecurityBusinessFunction.class, SecurityBusinessFunctionTable.class})
@RequiredArgsConstructor
public class SecurityBusinessFunctionController {

    private final ISecurityBusinessFunctionService businessFunctionService;

    @Operation(summary = "分页查询业务功能")
    @PostMapping("page")
    public R<IPage<BusinessFunctionVO>> page(@RequestBody BusinessFunctionQueryDTO query) {
        return R.ok(businessFunctionService.pageByCondition(query));
    }

    @Operation(summary = "查询所有业务功能")
    @GetMapping("listAll")
    public R<List<BusinessFunctionVO>> listAll() {
        return R.ok(businessFunctionService.listAll());
    }

    @Operation(summary = "根据ID查询业务功能详情")
    @GetMapping("{id}")
    public R<BusinessFunctionDetailVO> getDetailById(@PathVariable String id) {
        return R.ok(businessFunctionService.getDetailById(id));
    }

    @Operation(summary = "新增或更新业务功能")
    @PostMapping
    public R<String> saveOrUpdate(@RequestBody BusinessFunctionVO vo) {
        AssertUtils.hasText(vo.getName(), SecurityErrorCode.E06001);
        AssertUtils.hasText(vo.getSummary(), SecurityErrorCode.E06002);
        AssertUtils.hasText(vo.getDetail(), SecurityErrorCode.E06003);
        businessFunctionService.saveOrUpdateFunction(vo);
        return R.ok(vo.getId());
    }

    @Operation(summary = "批量删除业务功能")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(businessFunctionService.removeByIds(ids));
    }
}
