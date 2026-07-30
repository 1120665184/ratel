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
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
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
 * * 配置示例：
 * * org.quyq:
 * *   auth:
 * *     platform:
 * *       three-platform:
 * *         dingtalk:
 * *           client-id: ***
 * *           client-secret: ***
 * *           # 生成token后以重定向的形式返回数据，重定向地址见redirect-url
 * *           redirect: true
 * *           # 和redirect配合使用
 * *           redirect-url: [viewBaseUrl]/sub-system/login
 * *           properties:
 * *             # 钉钉服务器回调本系统的回调地址
 * *             redirect_uri: [apiBaseUrl]/system/auth/callback/manager/dingtalk
 */
@Slf4j
public abstract class DingTalkLoginHandler<T extends UserInfo> extends AbstractThreePlatformLoginHandler<T> {

    public static final String THREAD_DINGTALK_LOGIN_TYPE = "dingtalk";

    private static final String DING_TALK_AUTH_URL = "https://login.dingtalk.com/oauth2/auth";

    private static final String DING_TALK_ACCESS_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";

    private static final String DING_TALK_USER_INFO = "https://api.dingtalk.com/v1.0/contact/users/me";

    public static final String PARAM_CREATE_METHOD_KEY = "createMethod";

    public static final String PARAM_TEMPORARY_VOUCHER_KEY = "temporaryVoucher";
    public static final String PARAM_BINDING_TOKEN_KEY = "bindingToken";
    public static final String PARAM_USERNAME_KEY = "username";
    public static final String PARAM_PASSWORD_KEY = "password";

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
        MultiValueMap<String, String> extraParam = loginVO.getExtraParam();

        //三方平台已经调用，处理创建新账号，还是绑定用户
        if (!CollectionUtils.isEmpty(extraParam) && extraParam.containsKey(PARAM_CREATE_METHOD_KEY)) {
            String temporaryVoucher = requiredParam(extraParam, PARAM_TEMPORARY_VOUCHER_KEY);
            //不是重定向
            config.setRedirect(false);

            String createMethod = requiredParam(extraParam, PARAM_CREATE_METHOD_KEY);
            if ("create".equals(createMethod)) {
                return createNewAccount(temporaryVoucher, extraParam);
            } else if ("binding".equals(createMethod)) {
                return bindingUser(temporaryVoucher, extraParam);
            }
            throw new AuthException(CommonErrorCode.E03006, "未知的创建方式：" + createMethod);
        }

        String code = Optional.ofNullable(extraParam)
                .map(params -> params.get("authCode"))
                .map(List::getFirst).orElse(null);
        if (!StringUtils.hasText(code)) {
            String error = Optional.ofNullable(extraParam)
                    .map(params -> params.get("error"))
                    .map(List::getFirst).orElse(null);
            if (!StringUtils.hasText(error)) {
                error = "未获取到code";
            }
            throw new AuthException(CommonErrorCode.E03006, error);

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
        } catch (RestClientException e) {
            throw new AuthException(CommonErrorCode.E03006, "获取钉钉Access Token异常:" + e.getMessage());
        }


        try {
            DingTalkInfo body = restClient
                    .get()
                    .uri(DING_TALK_USER_INFO)
                    .header("x-acs-dingtalk-access-token", accessTokenValue.accessToken)
                    .retrieve()
                    .body(DingTalkInfo.class);

            if (Objects.isNull(body)) {
                throw new AuthException(CommonErrorCode.E03006, "没有获取到钉钉用户信息");
            }

            return getUserByDingTalkInfo(body);

        } catch (RestClientException e) {
            log.error(e.getMessage(), e);
            throw new AuthException(CommonErrorCode.E03006, "钉钉用户信息获取失败:" + e.getMessage());
        }

    }

    /**
     * 通过钉钉用户信息获取本系统的账号信息
     *
     * @param info
     * @return
     */
    protected abstract T getUserByDingTalkInfo(DingTalkInfo info);

    /**
     * 创建新账号
     * @param temporaryVoucher
     * @return
     */
    protected abstract T createNewAccount(String temporaryVoucher, MultiValueMap<String, String> extraParam);

    /**
     * 绑定已有账号
     * @param temporaryVoucher
     * @return
     */
    protected abstract T bindingUser(String temporaryVoucher, MultiValueMap<String, String> extraParam);

    protected String requiredParam(MultiValueMap<String, String> extraParam, String key) {
        List<String> values = extraParam.get(key);
        if (CollectionUtils.isEmpty(values) || values.size() != 1 || !StringUtils.hasText(values.getFirst())) {
            throw new AuthException(CommonErrorCode.E03006, "未携带有效参数：" + key);
        }
        return values.getFirst();
    }

    @Override
    public String loginType() {
        return THREAD_DINGTALK_LOGIN_TYPE;
    }

    record AccessTokenRes(
            String accessToken,
            String refreshToken,
            Long expireIn,
            String corpId
    ) {
    }


    protected record DingTalkInfo(
            String nick,
            String avatarUrl,
            String mobile,
            String openId,
            String unionId,
            String email
    ) {
    }

}
