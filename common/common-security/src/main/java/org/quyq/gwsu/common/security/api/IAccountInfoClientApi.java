package org.quyq.gwsu.common.security.api;


import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.factory.AccountInfoClientApiFallbackFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
@ApiClient(value = CoreConstants.Server.SYSTEM_NAME, note = "账号信息获取", fallbackFactory = AccountInfoClientApiFallbackFactory.class)
@HttpExchange("basic")
public interface IAccountInfoClientApi {

    /**
     * 通过钉钉unionId获取真实的用户ID
     * @param unionId
     * @return
     */
    @GetExchange("getUserIdByDingTalkUnionId/{unionId}")
    R<String> getUserIdByDingTalkUnionId(@PathVariable String unionId);
}
