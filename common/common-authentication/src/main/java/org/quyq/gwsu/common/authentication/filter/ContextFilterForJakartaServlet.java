/*
 * Copyright 2020-2099 sa-token.cc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quyq.gwsu.common.authentication.filter;

import cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil;
import cn.dev33.satoken.util.SaTokenConsts;
import cn.hutool.jwt.JWTUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * SaTokenContext 上下文初始化过滤器 (基于 Jakarta-Servlet)
 *
 * @author click33
 * @since 1.42.0
 */
@RequiredArgsConstructor
@Order(SaTokenConsts.SA_TOKEN_CONTEXT_FILTER_ORDER)
public class ContextFilterForJakartaServlet implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            var req = (HttpServletRequest) request;
            SaTokenContextJakartaServletUtil.setContext(req, (HttpServletResponse) response);
            String accountType = null;
            String token = getToken(req);
            if (StringUtils.hasText(token) && JWTUtil.verify(token,
                    SecurityConstants.JWT.AUTH_JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8))) {
                accountType = JWTUtil.parseToken(token)
                        .getPayloads().getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY);
            }
            ScopedValue.where(AuthenticationConstants.ACCOUNT_TYPE, Optional.ofNullable(accountType)
                            .map(AccountType::fromString).orElse(null))
                    .call(() -> {
                        chain.doFilter(request, response);
                        return 0;
                    });

        } catch (Exception e) {
            throw new BusinessException(e);
        } finally {
            SaTokenContextJakartaServletUtil.clearContext();
        }
    }


    private String getToken(HttpServletRequest request) {
        String authenInfo = request.getHeader(SecurityConstants.Authentication.HTTP_HEADER_TOKEN_KEY);
        if (StringUtils.hasText(authenInfo)) {
            return authenInfo.replace(SecurityConstants.Authentication.TOKEN_PREFIX, "");
        }

        return null;
    }

}
