package org.quyq.gwsu.common.job.config;

import com.google.gson.Gson;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.executor.XxlJobExecutor;
import org.quyq.gwsu.common.job.openapi.executor.ExecutorBiz;
import org.quyq.gwsu.common.job.openapi.executor.dto.IdleBeatRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.KillRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.common.job.openapi.executor.impl.ExecutorBizImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * 执行器Web端点配置
 * <p>
 * 仅分布式模式注册RouterFunction，暴露/job-executor/*端点供Admin调用
 */
@AutoConfiguration
@ConditionalOnClass({RouterFunction.class})
@ConditionalOnBean(XxlJobExecutor.class)
@ConditionalOnProperty(name = "deploy.single", havingValue = "false")
public class ExecutorWebConfiguration {

    private static final String BASE_PATH = "/job-executor";

    @Bean
    public RouterFunction<ServerResponse> jobExecutorRouters(Gson gson) {
        ExecutorBiz executorBiz = new ExecutorBizImpl();

        return RouterFunctions
                .route(RequestPredicates.POST(BASE_PATH + "/beat"), request -> {
                    R<String> result = executorBiz.beat();
                    return ServerResponse.ok().body(result);
                })
                .andRoute(RequestPredicates.POST(BASE_PATH + "/idleBeat"), request -> {
                    IdleBeatRequest req = gson.fromJson(request.body(String.class), IdleBeatRequest.class);
                    R<String> result = executorBiz.idleBeat(req);
                    return ServerResponse.ok().body(result);
                })
                .andRoute(RequestPredicates.POST(BASE_PATH + "/trigger"), request -> {
                    TriggerRequest req = gson.fromJson(request.body(String.class), TriggerRequest.class);
                    R<String> result = executorBiz.trigger(req);
                    return ServerResponse.ok().body(result);
                })
                .andRoute(RequestPredicates.POST(BASE_PATH + "/kill"), request -> {
                    KillRequest req = gson.fromJson(request.body(String.class), KillRequest.class);
                    R<String> result = executorBiz.kill(req);
                    return ServerResponse.ok().body(result);
                })
                .andRoute(RequestPredicates.POST(BASE_PATH + "/log"), request -> {
                    LogRequest req = gson.fromJson(request.body(String.class), LogRequest.class);
                    R<?> result = executorBiz.log(req);
                    return ServerResponse.ok().body(result);
                });
    }

}
