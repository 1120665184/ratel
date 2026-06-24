package org.quyq.gwsu.common.authentication.login.impl.dingtalk;


import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.login.AbstractThreePlatformLoginHandler;
import org.quyq.gwsu.common.authentication.login.domain.ThreePlatformConfig;
import org.quyq.gwsu.common.authentication.login.domain.ThreePlatformLoginDTO;
import org.quyq.gwsu.common.authentication.login.domain.WebCallInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description 钉钉三方快捷登录
 */
@Slf4j
public abstract class DingTalkLoginHandler<T extends UserInfo> extends AbstractThreePlatformLoginHandler<T> {

    public static final String THREAD_DINGTALK_LOGIN_TYPE = "dingtalk";

    private static final String DING_TALK_AUTH_URL = "https://login.dingtalk.com/oauth2/auth";

    private static final String DING_TALK_ACCESS_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";

    private static final String DING_TALK_USER_INFO = "https://api.dingtalk.com/v1.0/contact/users/me";

    private final RestClient restClient = RestClient.builder().build();

    @Override
    protected @NonNull WebCallInfo authUrl(ThreePlatformConfig config, String state) {

        Map<String, String> properties = config.getProperties();
        String authUrl = Optional.ofNullable(properties.get("auth_url")).orElse(DING_TALK_AUTH_URL);
        String scope = Optional.ofNullable(properties.get("scope")).orElse("openid");
        String prompt = "consent";
        String urlState = properties.get("state");
        String redirectUri = properties.get("redirect_uri");
        AssertUtils.hasText(redirectUri, CommonErrorCode.E03005);
        StringBuilder url = new StringBuilder(authUrl).append("?response_type=code")
                .append("&client_id=").append(config.getClientId())
                .append("&scope=").append(scope)
                .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
                .append("&prompt=").append(prompt);
        if (StringUtils.hasText(urlState)) {
            url.append("&state=").append(urlState);
        }

        return new WebCallInfo(url.toString(), null);
    }

    @Override
    protected T callback(ThreePlatformLoginDTO loginVO, ThreePlatformConfig config) {
        String code = Optional.ofNullable(loginVO.getExtraParam().get("authCode"))
                .map(List::getFirst).orElse(null);
        if(!StringUtils.hasText(code)){
            String error = Optional.ofNullable(loginVO.getExtraParam().get("error"))
                    .map(List::getFirst).orElse(null);
            if(!StringUtils.hasText(error)){
                error = "未获取到code";
            }
            throw new AuthException(CommonErrorCode.E03006 , error);

        }

        String accessTokenUrl = Optional.ofNullable(config.getProperties().get("access_token_url")).orElse(DING_TALK_ACCESS_TOKEN_URL);

        AccessTokenRes accessTokenValue;
        try {
            accessTokenValue = restClient
                    .post()
                    .uri(accessTokenUrl)
                    .body(Map.of("clientId", config.getClientId(),
                            "clientSecret", config.getClientSecret(),
                            "code", code,
                            "grantType", "authorization_code"))
                    .retrieve()
                    .body(AccessTokenRes.class);
        }catch (RestClientException e){
            throw new AuthException(CommonErrorCode.E03006 , "获取钉钉Access Token异常:"+e.getMessage());
        }


        try {
            DingTalkInfo body = restClient
                    .get()
                    .uri(DING_TALK_USER_INFO)
                    .header("x-acs-dingtalk-access-token", accessTokenValue.accessToken)
                    .retrieve()
                    .body(DingTalkInfo.class);

            if(Objects.isNull(body)){
                throw new AuthException(CommonErrorCode.E03006 , "没有获取到钉钉用户信息");
            }

            return getUserByDingTalkInfo(body);

        }catch (RestClientException e){
            log.error(e.getMessage(), e);
            throw new AuthException(CommonErrorCode.E03006 , "钉钉用户信息获取失败:" +e.getMessage());
        }

    }

    /**
     * 通过钉钉用户信息获取本信息的信息
     * @param info
     * @return
     */
    protected abstract T getUserByDingTalkInfo(DingTalkInfo info);

    @Override
    public String loginType() {
        return THREAD_DINGTALK_LOGIN_TYPE;
    }

    record AccessTokenRes(
            String accessToken,
            String refreshToken,
            Long expireIn,
            String corpId
    ){}


    protected record DingTalkInfo(
            String nick ,
            String avatarUrl,
            String mobile,
            String openId,
            String unionId,
            String email
    ){}

}
