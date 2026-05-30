package org.quyq.gwsu.security.dict.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.api.IDictInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.DictValueVO;
import org.quyq.gwsu.security.api.dict.dto.DictQueryDTO;
import org.quyq.gwsu.security.api.dict.dto.DictSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictVO;
import org.quyq.gwsu.security.dict.domain.SecurityDict;
import org.quyq.gwsu.security.dict.domain.SecurityDictValue;
import org.quyq.gwsu.security.dict.service.ISecurityDictService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("dict")
@Tag(name = "字典管理", description = "字典管理接口")
@TableModelPermission({SecurityDict.class, SecurityDictValue.class})
@RequiredArgsConstructor
public class SecurityDictController implements IDictInfoClientApi {

    private final ISecurityDictService dictService;

    @Operation(summary = "根据ID查询字典")
    @GetMapping("/{id}")
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


    @Operation(summary = "通过字典键获取值列表")
    @GetMapping("dictValue/get/{dictKey}")
    public R<List<DictValueVO>> getDictValueByDictKey(@PathVariable String dictKey) {
        return R.ok(dictService.getByDictKey(dictKey));
    }

    @Operation(summary = "批量获取字典数据")
    @PostMapping("dictValue/getBatch")
    @LoginAllowAccess
    @Override
    public R<Map<String, List<DictValueVO>>> getDictValueByDictKeyBatch(@RequestBody List<String> dictKeys) {
        if (CollectionUtils.isEmpty(dictKeys)) {
            return R.ok(Map.of());
        }

        Map<String, List<DictValueVO>> finV = new LinkedHashMap<>();

        for (String dictKey : dictKeys) {
            finV.put(dictKey, dictService.getByDictKey(dictKey));
        }
        return R.ok(finV);
    }

    @Operation(summary = "保存或更新字典值")
    @PostMapping("dictValue/saveOrUpdate")
    public R<Boolean> saveOrUpdateDictValue(@RequestBody SecurityDictValue dto) {
        return R.ok(dictService.saveOrUpdateDictValue(dto));
    }

    @Operation(summary = "批量删除字典值")
    @DeleteMapping("dictValue/removes")
    public R<Boolean> removeDictValueByDictKey(@RequestBody List<String> ids) {
        return R.ok(dictService.removeDictValueByIds(ids));
    }

    @Operation(summary = "给字典值排序")
    @PostMapping("dictValue/sort/{dictKey}")
    public R<Boolean> sortDictValue(@PathVariable String dictKey, @RequestBody List<String> dictValueIds) {
        return R.ok(dictService.updateDictValueSort(dictKey, dictValueIds));
    }

}
