package org.quyq.gwsu.common.deploy.filter;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.exception.ExceptionMsgHandler;
import org.quyq.gwsu.common.core.utils.filter.ProcessorChain;
import org.quyq.gwsu.common.core.utils.filter.RequestResponseContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 * @author Quyq
 * @date 2026/4/1
 * @description 单机版处理过滤器处理逻辑
 */
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SingleProcessorFilter implements Filter {

    private final ProcessorChain processorChain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        RequestResponseContext context = buildContext(httpRequest);
        try {
            // 1. 执行 preHandle 链
            Boolean preSuccess = processorChain.executePreHandlers(context).block();

            if (preSuccess == null || !preSuccess) {
                applyResponse(context, httpResponse);
                return;
            }

            // 2. 执行业务，若需要捕获响应体则使用包装器
            ContentCachingResponseWrapper wrapper = null;
            HttpServletResponse responseToUse = httpResponse;
            if (processorChain.isAnyNeedsResponseBody(context)) {
                wrapper = new ContentCachingResponseWrapper(httpResponse);
                responseToUse = wrapper;
            }

            try {
                chain.doFilter(request, responseToUse);
            } finally {
                if (wrapper != null) {
                    // 捕获原始响应体
                    byte[] content = wrapper.getContentAsByteArray();
                    String responseBody = new String(content, httpResponse.getCharacterEncoding());
                    context.setOriginalResponseBody(responseBody);
                }
            }

            // 3. 执行 postHandle 链（此时处理器可读取 context.getOriginalResponseBody() 并设置 context.setModifiedResponseBody(...)）
            processorChain.executePostHandlers(context).block();

            // 4. 应用响应（包括可能的响应体替换）
            applyResponse(context, httpResponse);

            // 5. 写出响应体（优先使用修改后的，否则使用原始捕获的）
            if (context.getModifiedResponseBody() != null) {
                httpResponse.getOutputStream().write(context.getModifiedResponseBody().toString().getBytes(httpResponse.getCharacterEncoding()));
                httpResponse.getOutputStream().flush();
            } else if (wrapper != null) {
                wrapper.copyBodyToResponse();
            }
        } catch (Exception e) {
            handleException(e, httpResponse);
        }


    }

    /**
     * 处理 Filter 层抛出的异常
     */
    private void handleException(Exception ex, HttpServletResponse response) throws IOException {
        ExceptionMsgHandler.ErrorInfo errorInfo = ExceptionMsgHandler.determineErrorInfo(ex);
        response.setStatus(errorInfo.code().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorInfo.result()));
        response.getWriter().flush();
    }


    private RequestResponseContext buildContext(HttpServletRequest request) {
        // 请求头转换
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        serverRequest.getHeaders().forEach((k ,v) -> headers.put(k.toLowerCase(Locale.ROOT) , v));

        // 查询参数转换
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.putAll(UriComponentsBuilder.fromUriString(request.getRequestURI())
                .query(request.getQueryString())
                .build()
                .getQueryParams());

        return new RequestResponseContext(
                request.getRequestURI(),
                request.getMethod(),
                headers,
                queryParams
        );


    }


    private void applyResponse(RequestResponseContext context, HttpServletResponse response) {
        response.setStatus(context.getHttpStatus());
        context.getResponseHeaders().forEach((name, values) -> {
            for (String value : values) {
                response.setHeader(name, value);
            }
        });
    }

}
