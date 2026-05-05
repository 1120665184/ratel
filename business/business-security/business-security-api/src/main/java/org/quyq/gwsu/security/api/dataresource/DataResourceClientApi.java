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


}
