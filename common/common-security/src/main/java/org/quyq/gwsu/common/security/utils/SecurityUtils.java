package org.quyq.gwsu.common.security.utils;


import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWTUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.config.GsonConfiguration;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.domain.deserializer.JacksonCompatibleTypeAdapterFactory;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/4/9
 * @description
 */
@RequiredArgsConstructor
public class SecurityUtils {
    private final CacheUtils cacheUtils;
    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, GsonConfiguration.jsonSerializerDateTime)
                .registerTypeAdapter(LocalDate.class, GsonConfiguration.jsonSerializerDate)
                .registerTypeAdapter(LocalDateTime.class, GsonConfiguration.jsonDeserializerDateTime)
                .registerTypeAdapter(LocalDate.class, GsonConfiguration.jsonDeserializerDate)
                .registerTypeAdapterFactory(new JacksonCompatibleTypeAdapterFactory())
                .create();
    }

    /**
     * 获取当前登录用户的username
     *
     * @return
     */
    public String getUsername() {
        return Optional.ofNullable(ServletUtils.getHeaders())
                .map(h -> h.get(CoreConstants.Headers.AUTHORIZATION_USER_NAME))
                .orElse(null);
    }

    /**
     * 获取Token
     *
     * @return
     */
    public String getToken() {
        return Optional.ofNullable(ServletUtils.getHeaders())
                .map(headers -> headers.get(CoreConstants.Headers.HTTP_HEADER_TOKEN_KEY))
                .map(token -> token.replace(CoreConstants.Headers.TOKEN_PREFIX, ""))
                .map(token -> {
                    if (JWTUtil.verify(token, SecurityConstants.JWT.AUTH_JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8))) {
                        return token;
                    }
                    return null;
                })
                .orElse(null);
    }


    /**
     * 获取登录的客户端信息
     *
     * @param <U>
     * @return
     */
    public <U extends ClientInfo> Optional<U> clientInfo() {
        return clientInfo(getToken());
    }

    /**
     * 获取登录的客户端信息
     *
     * @param token
     * @param <U>
     * @return
     */
    public <U extends ClientInfo> Optional<U> clientInfo(String token) {
        return getSubject(token)
                .flatMap(Subject::clientInfo);

    }

    /**
     * 获取登录的用户信息
     *
     * @param <U>
     * @return
     */
    public <U extends UserInfo> Optional<U> userInfo() {
        return userInfo(getToken());
    }

    /**
     * 获取登录的用户信息
     *
     * @param token
     * @param <U>
     * @return
     */
    public <U extends UserInfo> Optional<U> userInfo(String token) {
        return getSubject(token)
                .flatMap(Subject::userInfo);
    }

    /**
     * 获取当前登录主体
     *
     * @param <U>
     * @return
     */
    public <U extends Visitor> Optional<Subject<U>> getSubject() {
        return getSubject(getToken());
    }

    /**
     * 获取当前登录主体
     *
     * @param token
     * @param <U>
     * @return
     */
    public <U extends Visitor> Optional<Subject<U>> getSubject(String token) {
        if (!StringUtils.hasText(token) ||
                !JWTUtil.verify(token, SecurityConstants.JWT.AUTH_JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        JSONObject payloads = JWTUtil.parseToken(token).getPayloads();
        return cacheUtils.withRebel(() ->
                Optional.ofNullable(payloads)
                        .map(payload -> payload.getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY))
                        //先校验TOKEN是否已过期
                        .map(loginType -> cacheUtils.get(SecurityConstants.Authentication.TOKEN_SPLICING_KEY_VALUE.apply(loginType) + token))
                        .map(Object::toString)
                        .map(loginId -> cacheUtils.get(SecurityConstants.Authentication.TOKEN_SPLICING_KEY_SESSION.apply(payloads.getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY)) + loginId))
                        .map(Object::toString)
                        .map(session -> JsonParser.parseString(session).getAsJsonObject())
                        .map(session -> session.getAsJsonObject(SecurityConstants.Session.SESSION_ROOT_MAP_NAME_KEY))
                        .map(data -> data.get(SecurityConstants.Session.SESSION_SUBJECT_INFO_KEY))
                        .map(user -> gson.fromJson(user, new TypeToken<Subject<Visitor>>() {
                        }.getType()))

        );

    }


}
