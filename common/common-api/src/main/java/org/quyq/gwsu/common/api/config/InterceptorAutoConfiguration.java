package org.quyq.gwsu.common.api.config;


import org.quyq.gwsu.common.api.interceptor.ApiClientWebClientFilter;
import org.quyq.gwsu.common.core.interceptor.ApiClientInterceptor;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestAttributes;

import java.util.Locale;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/12
 * @description
 */
@AutoConfiguration
public class InterceptorAutoConfiguration {

    @Bean
    @ConditionalOnClass(value = {RequestAttributes.class})
    public ApiClientInterceptor commonHeaderApiClientInterceptor(ProjectUtils projectUtils) {
        return (headers) -> {
            Map<String, String> currHeaders = ServletUtils.getHeaders();

            currHeaders.forEach((k, v) -> {
                if (!CoreConstants.Headers.REQUEST_IGNORE_HEADER.contains(k.toLowerCase(Locale.ROOT)))
                    headers.add(k, v);

                headers.remove(CoreConstants.Headers.SERVER_FROM_APP);
                headers.add(CoreConstants.Headers.SERVER_FROM_APP , projectUtils.getApplicationName());
            });
        };
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    public ApiClientWebClientFilter commonHeaderApiClientWebClientFilter(ProjectUtils projectUtils) {
        return (requestBuilder) -> {
            Map<String, String> currHeaders = ServletUtils.getHeaders();

            currHeaders.forEach((k, v) -> {
                if (!CoreConstants.Headers.REQUEST_IGNORE_HEADER.contains(k.toLowerCase(Locale.ROOT)))
                    requestBuilder.header(k, v);
            });

            requestBuilder.headers(headers -> {
                headers.remove(CoreConstants.Headers.SERVER_FROM_APP);
                headers.add(CoreConstants.Headers.SERVER_FROM_APP, projectUtils.getApplicationName());
            });
        };
    }

}
