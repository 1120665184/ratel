package org.quyq.gwsu.security.api.dataresource;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceQueryDTO;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceSaveDTO;
import org.quyq.gwsu.security.api.dataresource.fallback.DataResourceClientApiFallbackFactory;
import org.quyq.gwsu.security.api.dataresource.vo.DataResourceVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 数据资源配置 API 客户端接口
 *
 * @author Quyq
 * @date 2026/4/20
 */
@ApiClient(value = "gwsu-security", note = "数据资源配置API", fallbackFactory = DataResourceClientApiFallbackFactory.class)
@HttpExchange("/security/data-resource")
public interface DataResourceClientApi {

    /**
     * 根据ID查询数据资源配置
     */
    @GetExchange("/{id}")
    R<DataResourceVO> getById(@PathVariable("id") Long id);

    /**
     * 根据表名查询数据资源配置列表
     */
    @GetExchange("/by-table/{tableName}")
    R<List<DataResourceVO>> listByTableName(@PathVariable("tableName") String tableName);

    /**
     * 新增或更新数据资源配置
     */
    @PostExchange
    R<Boolean> saveOrUpdate(@RequestBody DataResourceSaveDTO dto);

    /**
     * 批量删除数据资源配置
     */
    @PostExchange("/delete")
    R<Boolean> removeByIds(@RequestBody List<Long> ids);

    /**
     * 同步数据资源规则到Redis
     */
    @PostExchange("/sync")
    R<Boolean> syncToRedis();

}
