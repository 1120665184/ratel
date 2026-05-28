package org.quyq.gwsu.security.dict.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.dict.dto.DictValueSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictValueVO;
import org.quyq.gwsu.security.dict.service.ISecurityDictValueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典值管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("dict-value")
@Tag(name = "字典值管理", description = "字典值管理接口")
@RequiredArgsConstructor
public class SecurityDictValueController {

    private final ISecurityDictValueService dictValueService;

    @Operation(summary = "查询字典下的值列表")
    @GetMapping("/list/{dictId}")
    public R<List<DictValueVO>> listByDictId(@PathVariable String dictId) {
        return R.ok(dictValueService.listByDictId(dictId));
    }

    @Operation(summary = "新增或更新字典值")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody DictValueSaveDTO dto) {
        return R.ok(dictValueService.saveOrUpdateValue(dto));
    }

    @Operation(summary = "批量删除字典值")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(dictValueService.removeByIds(ids));
    }

    @Operation(summary = "更新排序")
    @PutMapping("/sort")
    public R<Boolean> updateSort(@RequestBody List<String> ids) {
        return R.ok(dictValueService.updateSort(ids));
    }
}
