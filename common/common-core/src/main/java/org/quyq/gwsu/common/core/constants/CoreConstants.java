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

        /**
         * 获取微服务信息列表
         */
        String ENDPOINT_MODULE_INFOS = "/modules/list";

        /**
         * 查询数据库接口 ， 该接口只有微服务模式启用
         */
        String ENDPOINT_DB_EXECUTION = "/db/query";

        /**
         * 查询所有数据源列表
         */
        String ENDPOINT_DB_DATASOURCE = "/db/datasource";

        /**
         * 查询指定数据源中库的表信息
         */
        String ENDPOINT_DB_TABLES = "/db/tables";

        /**
         * 查询指定表的的列信息
         */
        String ENDPOINT_DB_COLUMNS = "/db/columns";

        /**
         * 获取指定数据源的数据名
         */
        String ENDPOINT_DB_NAME = "/db/name";

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
