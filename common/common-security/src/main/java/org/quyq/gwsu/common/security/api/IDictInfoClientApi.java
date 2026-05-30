package org.quyq.gwsu.common.security.api;


import io.swagger.v3.oas.annotations.Operation;
import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.api.factory.DictInfoClientApiFallbackFactory;
import org.quyq.gwsu.common.security.api.vo.DictValueVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description 获取字典信息远程接口
 */
@ApiClient(value = CoreConstants.Server.SECURITY_NAME, note = "字典信息获取", fallbackFactory = DictInfoClientApiFallbackFactory.class)
@HttpExchange("dict")
public interface IDictInfoClientApi {


    @Operation(summary = "批量获取字典数据")
    @PostExchange("dictValue/getBatch")
    R<Map<String, List<DictValueVO>>> getDictValueByDictKeyBatch(@RequestBody List<String> dictKeys);
}
