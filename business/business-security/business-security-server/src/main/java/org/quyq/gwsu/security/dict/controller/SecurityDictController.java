package org.quyq.gwsu.security.dict.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.dict.SecurityDictClientApi;
import org.quyq.gwsu.security.api.dict.dto.DictQueryDTO;
import org.quyq.gwsu.security.api.dict.dto.DictSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictVO;
import org.quyq.gwsu.security.dict.service.ISecurityDictService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("dict")
@Tag(name = "字典管理", description = "字典管理接口")
@RequiredArgsConstructor
public class SecurityDictController implements SecurityDictClientApi {

    private final ISecurityDictService dictService;

    @Operation(summary = "根据ID查询字典")
    @GetMapping("/{id}")
    @Override
    public R<DictVO> getById(@PathVariable String id) {
        return R.ok(dictService.getById(id));
    }

    @Operation(summary = "分页查询字典")
    @PostMapping("/page")
    public R<IPage<DictVO>> page(@RequestBody DictQueryDTO query) {
        return R.ok(dictService.pageByCondition(query));
    }

    @Operation(summary = "新增或更新字典")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody DictSaveDTO dto) {
        return R.ok(dictService.saveOrUpdateDict(dto));
    }

    @Operation(summary = "批量删除字典")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(dictService.removeByIds(ids));
    }
}
