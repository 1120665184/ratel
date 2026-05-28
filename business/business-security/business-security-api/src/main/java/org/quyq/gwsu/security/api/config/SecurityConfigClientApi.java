package org.quyq.gwsu.security.api.config;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.config.fallback.SecurityConfigClientApiFallbackFactory;
import org.quyq.gwsu.security.api.config.vo.ConfigVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 配置管理 API 客户端接口
 *
 * @author Quyq
 */
@ApiClient(value = "gwsu-security", note = "配置管理API", fallbackFactory = SecurityConfigClientApiFallbackFactory.class)
@HttpExchange("/security/config")
public interface SecurityConfigClientApi {

    /**
     * 根据ID查询配置
     */
    @GetExchange("/{id}")
    R<ConfigVO> getById(@PathVariable("id") String id);

    /**
     * 根据配置键查询配置
     */
    @GetExchange("/key/{configKey}")
    R<ConfigVO> getByKey(@PathVariable("configKey") String configKey);

}
