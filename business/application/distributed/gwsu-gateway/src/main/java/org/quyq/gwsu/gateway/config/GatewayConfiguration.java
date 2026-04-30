package org.quyq.gwsu.gateway.config;

import org.quyq.gwsu.gateway.exception.GatewayErrorWebExceptionHandler;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

@Configuration
public class GatewayConfiguration {

    private final WebProperties webProperties;
    private final ApplicationContext applicationContext;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public GatewayConfiguration(
            WebProperties webProperties,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {
        this.webProperties = webProperties;
        this.applicationContext = applicationContext;
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    @Bean
    @Order(-1)
    public GatewayErrorWebExceptionHandler gatewayErrorWebExceptionHandler(
            ErrorAttributes errorAttributes) {
        GatewayErrorWebExceptionHandler exceptionHandler = new GatewayErrorWebExceptionHandler(
                errorAttributes,
                webProperties.getResources(),
                new ErrorProperties(),
                applicationContext
        );
        exceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
        exceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());
        return exceptionHandler;
    }
}
