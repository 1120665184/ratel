package org.quyq.gwsu.common.core.constants;


import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * @author Quyq
 * @date 2026/3/10
 * @description 公共常量
 */
public interface CoreConstants {

    interface Project {
        String COMMON_PACKAGE = "org.quyq.gwsu";
    }


    interface Yaml {

        String PROJECT_CONFIG_PREFIX = "org.quyq";

        String APPLICATION_NAME = "spring.application.name";

        String DEPLOY_SINGLE = "deploy.single";

    }

    interface EndPoint {

        String ENDPOINT_MODULE_INFOS = "/modules/list";

    }

    interface Headers {

        /**
         * 服务调用请求头传递时忽略的内容
         */
        List<String> REQUEST_IGNORE_HEADER = Arrays.asList("content-length", "connection", "origin", "cookie", "accept", "request-origion", "referer", //NOSONAR
                "host", "forwarded", "content-md5", "cache-control", "etag", "server", "accept-encoding", "content-encoding", "transfer-encoding");

    }


    interface Server {

        String SECURITY_NAME = "gwsu-security";

        String SYSTEM_NAME = "gwsu-system";

    }


    @Getter
    enum Code {
        SUCCESS(200, "操作成功"),
        ERROR(500, "操作失败");

        private final String msg;
        private final int code;

        Code(int code, String msg) {
            this.code = code;
            this.msg = msg;

        }


    }

}
