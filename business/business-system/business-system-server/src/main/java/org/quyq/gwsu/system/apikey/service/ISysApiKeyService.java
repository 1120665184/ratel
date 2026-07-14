package org.quyq.gwsu.system.apikey.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginUserVO;
import org.quyq.gwsu.system.api.apikey.dto.ApiKeyCreateDTO;
import org.quyq.gwsu.system.api.apikey.dto.ApiKeyQueryDTO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyCreateResultVO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyDetailVO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyVO;
import org.quyq.gwsu.system.apikey.domain.SysApiKey;

/**
 * API_KEY Service
 *
 * @author Quyq
 */
public interface ISysApiKeyService extends IService<SysApiKey> {

    String CACHE_API_KEY_PREFIX = "sys-api-key";

    ApiKeyCreateResultVO createCurrentUserApiKey(ApiKeyCreateDTO dto);

    IPage<ApiKeyVO> pageCurrentUserApiKeys(ApiKeyQueryDTO query);

    ApiKeyDetailVO getCurrentUserApiKeyDetail(String id);

    Boolean removeCurrentUserApiKey(String id);

    ApiKeyLoginUserVO validateApiKeyAndLoadUser(String apiKey);

    void refreshApiKeyLastUsed(String apiKeyId, String requestIp);
}
