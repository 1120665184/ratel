package org.quyq.gwsu.security.dataresource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.enums.DataResourceAssertType;
import org.quyq.gwsu.common.security.enums.DataResourceFieldConditionType;
import org.quyq.gwsu.security.api.dataresource.DataResourceClientApi;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceQueryDTO;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceSaveDTO;
import org.quyq.gwsu.security.api.dataresource.vo.DataResourceVO;
import org.quyq.gwsu.security.api.dataresource.vo.StringEnumOptionVO;
import org.quyq.gwsu.security.dataresource.service.ISecurityDataResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据资源配置控制器
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Tag(name = "数据资源配置")
@RestController
@RequestMapping("data-resource")
@RequiredArgsConstructor
public class SecurityDataResourceController implements DataResourceClientApi {

    private final ISecurityDataResourceService dataResourceService;

    @Operation(summary = "根据ID查询数据资源配置")
    @GetMapping("/{id}")
    @Override
    public R<DataResourceVO> getById(@PathVariable Long id) {
        return R.ok(dataResourceService.getById(id));
    }

    @Operation(summary = "根据表名查询数据资源配置列表")
    @GetMapping("/by-table/{tableName}")
    public R<List<DataResourceVO>> listByTableName(@PathVariable String tableName) {
        return R.ok(dataResourceService.listByTableName(tableName));
    }

    @Operation(summary = "分页查询数据资源配置")
    @PostMapping("/page")
    public R<IPage<DataResourceVO>> page(@RequestBody DataResourceQueryDTO query) {
        return R.ok(dataResourceService.pageByCondition(query));
    }

    @Operation(summary = "新增或更新数据资源配置")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody DataResourceSaveDTO dto) {
        return R.ok(dataResourceService.saveOrUpdate(dto));
    }

    @Operation(summary = "批量删除数据资源配置")
    @PostMapping("/delete")
    public R<Boolean> removeByIds(@RequestBody List<String> ids) {
        return R.ok(dataResourceService.removeByIds(ids));
    }

    @Operation(summary = "同步数据资源规则到Redis")
    @PostMapping("/sync")
    public R<Boolean> syncToRedis() {
        return R.ok(dataResourceService.syncToRedis());
    }

    @Operation(summary = "获取断言类型枚举选项")
    @GetMapping("/enums/assert-type")
    public R<List<StringEnumOptionVO>> assertTypeOptions() {
        return R.ok(java.util.Arrays.stream(DataResourceAssertType.values())
                .map(e -> new StringEnumOptionVO(e.getDescription(), e.name()))
                .toList());
    }

    @Operation(summary = "获取条件关联关系枚举选项")
    @GetMapping("/enums/condition-type")
    public R<List<StringEnumOptionVO>> conditionTypeOptions() {
        return R.ok(java.util.Arrays.stream(DataResourceFieldConditionType.values())
                .map(e -> new StringEnumOptionVO(e.getDescription(), e.name()))
                .toList());
    }

}
