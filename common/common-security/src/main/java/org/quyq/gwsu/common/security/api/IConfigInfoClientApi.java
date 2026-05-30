package org.quyq.gwsu.common.security.api;


import io.swagger.v3.oas.annotations.Operation;
import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.factory.ConfigInfoClientApiFallbackFactory;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description
 */
@ApiClient(value = CoreConstants.Server.SECURITY_NAME, note = "配置信息获取", fallbackFactory = ConfigInfoClientApiFallbackFactory.class)
@HttpExchange("config")
public interface IConfigInfoClientApi {

    @Operation(summary = "根据键查询配置")
    @PostExchange("key/get")
    R<Map<String, ConfigVO>> getByKeys(@RequestBody List<String> keys);

}
