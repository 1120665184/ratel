package org.quyq.gwsu.common.security.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author Quyq
 * @date 2024/4/29
 * @description 统一全局过滤器
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PropertiesSettingFilter implements Filter {

    private final SecurityUtils securityUtils;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, jakarta.servlet.FilterChain filterChain) throws IOException, ServletException {
        try {
            Map<String, String> headers = ServletUtils.getHeaders((HttpServletRequest) servletRequest);

            securityUtils.getSubject(getToken(headers)).ifPresent(subject -> putUserName(subject, headers));

            ServletUtils.LOCAL_HEADERS.set(headers);
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            //清除当前线程头部信息
            ServletUtils.LOCAL_HEADERS.remove();
        }
    }


    private void putUserName(Subject<Visitor> subject, Map<String, String> headers) {
        if (VisitorType.USER == subject.getSubjectType()) {
            subject.userInfo().ifPresent(userInfo ->
                    headers.put(SecurityConstants.Authentication.AUTHORIZATION_USER_NAME, userInfo.getUserName())
            );
        } else {
            subject.clientInfo().ifPresent(clientInfo ->
                    headers.put(SecurityConstants.Authentication.AUTHORIZATION_USER_NAME, clientInfo.getClientName())
            );
        }

    }


    private String getToken(Map<String, String> headers) {
        String authenInfo = headers.get(SecurityConstants.Authentication.HTTP_HEADER_TOKEN_KEY);
        if (StringUtils.hasText(authenInfo)) {
            return authenInfo.replace(SecurityConstants.Authentication.TOKEN_PREFIX, "");
        }

        return null;
    }


}
