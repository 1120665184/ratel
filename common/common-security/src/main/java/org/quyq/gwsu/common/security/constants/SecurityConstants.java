package org.quyq.gwsu.common.security.constants;


import java.util.function.UnaryOperator;

/**
 * @author Quyq
 * @date 2026/4/4
 * @description
 */
public interface SecurityConstants {

    interface Abac {

        /**
         * 权限数据缓存key
         */
        String PERMISSION_DATA_CACHE_KEY = "permission:list";

        /**
         * 字段权限缓存key
         */
        String PERMISSION_FIELD_CACHE_KEY = "permission:field";

        /**
         * 权限变更通知topic
         */
        String PERMISSION_CHANGE_NOTICE_TOPIC = "permission:change";

    }

    /**
     * 认证相关常量
     */
    interface Authentication {

        /**
         * 认证服务前缀
         */
        String AUTH_SERVER_PREFIX = "system";

        /**
         * 认证信息存储前缀
         */
        String AUTH_INFO_KEY_PREFIX = "authentication";


        /**
         * 拼接： 在保存 token - id 映射关系时，应该使用的key
         */
        UnaryOperator<String> TOKEN_SPLICING_KEY_VALUE = loginType -> AUTH_INFO_KEY_PREFIX + ":" + loginType + ":token:";
        /**
         * 拼接： 在保存 Account-Session 时，应该使用的 key
         */
        UnaryOperator<String> TOKEN_SPLICING_KEY_SESSION = loginType -> AUTH_INFO_KEY_PREFIX + ":" + loginType + ":session:";

        /**
         * 拼接：在保存 Token-Session 时，应该使用的 key
         */
        UnaryOperator<String> TOKEN_SPLICING_KEY_TOKEN_SESSION = loginType -> AUTH_INFO_KEY_PREFIX + ":" + loginType + ":token-session:";


        /**
         * 超级管理员角色标识
         */
        String ROLE_SUPER_ADMIN_FLAG = "super_admin";

        /**
         * 通用角色标识，所有用户都默认有该角色
         */
        String ROLE_COMMON_FLAG = "common";


    }

    interface JWT {
        /**
         * jwt认证令牌key
         */
        String AUTH_JWT_SECRET_KEY = "auth:jwt:gwsu";
        /**
         * loginId key
         */
        String LOGIN_ID_KEY = "loginId";
        /**
         * loginType key
         */
        String LOGIN_TYPE_KEY = "loginType";

        String LOGIN_CREATE_MILLIS_KEY = "createMillis";

    }

    interface Session {

        String SESSION_ROOT_MAP_NAME_KEY = "dataMap";
        /**
         * 登录主体信息
         */
        String SESSION_SUBJECT_INFO_KEY = "$$subject";

        /**
         * 当前工作区存储key
         */
        String SESSION_CURR_WORKSPACE = "$$workspace";

        /**
         * 当前数据资源
         */
        String SESSION_CURR_DATA_RESOURCE = "$$dataResource";
    }

    interface DataResource {

        /**
         * 数据资源规则缓存key
         */
        String DATA_RESOURCE_RULES_CACHE_KEY = "dataResource:rules";
    }

}
