package org.quyq.gwsu.security.api.dict;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.dict.fallback.SecurityDictClientApiFallbackFactory;
import org.quyq.gwsu.security.api.dict.vo.DictVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 字典管理 API 客户端接口
 *
 * @author Quyq
 */
@ApiClient(value = "gwsu-security", note = "字典管理API", fallbackFactory = SecurityDictClientApiFallbackFactory.class)
@HttpExchange("/security/dict")
public interface SecurityDictClientApi {

    /**
     * 根据ID查询字典
     */
    @GetExchange("/{id}")
    R<DictVO> getById(@PathVariable("id") String id);

}
