package org.quyq.gwsu.security.connect.entrance.dingtalk.utils;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.security.connect.domain.EntranceConfig;
import org.quyq.gwsu.security.connect.entrance.dingtalk.DingTalkClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Quyq
 * @date 2025/12/19
 * @description
 */
@Component
@RequiredArgsConstructor
public class DingtalkAccessTokenUtils {

    private final RestClient restClient = RestClient.builder().build();

    private final Map<String, AccessTokenWrapper> currToken = new ConcurrentHashMap<>();

    /**
     * 获取accessToken
     *
     * @return
     */
    public String getAccessToken() {
        EntranceConfig.DingTalk config = DingTalkClient.getDingTalkConfig();
        String clientId = config.getClientId();
        if (Objects.nonNull(currToken.get(clientId)) && notExpires(currToken.get(clientId))) {
            return currToken.get(clientId).accessToken();
        }
        synchronized (DingtalkAccessTokenUtils.class) {
            if (Objects.nonNull(currToken.get(clientId)) && notExpires(currToken.get(clientId))) {
                return currToken.get(clientId).accessToken();
            }


            String url = config.apiDomain() + "/v1.0/oauth2/accessToken";
            try {
                AccessTokenWrapper body = restClient
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "appKey", clientId,
                                "appSecret", config.getClientSecret()
                        ))
                        .retrieve()
                        .body(AccessTokenWrapper.class);
                if (Objects.isNull(body)) {
                    throw new BusinessException("token获取错误");
                }
                currToken.put(clientId, body);
                return body.accessToken();
            } catch (RestClientException ex) {
                throw new BusinessException("获取Token异常:" + ex.getMessage());
            }


        }

    }


    private static boolean notExpires(AccessTokenWrapper token) {
        Long getTime = token.createTime();
        int expiresIn = token.expireIn(); //过期时间，秒

        return System.currentTimeMillis() - getTime < expiresIn * 1000L;

    }


    record AccessTokenWrapper(
            String accessToken,
            int expireIn,
            Long createTime

    ) {
        public AccessTokenWrapper {
            if (Objects.isNull(createTime)) {
                createTime = System.currentTimeMillis();
            }
        }
    }


}
