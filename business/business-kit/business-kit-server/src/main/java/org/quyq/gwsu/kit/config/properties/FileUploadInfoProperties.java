package org.quyq.gwsu.kit.config.properties;

import lombok.Data;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;

/**
 * @author Quyq
 * @date 2024/5/23
 * @description 文件上传配置属性
 */
@Data
public class FileUploadInfoProperties {

    /**
     * 上传类型
     */
    private FileServiceType type;

    /**
     * 文件组
     */
    private String group;

    /**
     * 本地上传配置
     */
    private Local local = new Local();

    /**
     * minio配置
     */
    private Minio minio = new Minio();

    /**
     * oss配置
     */
    private Oss oss = new Oss();

    /**
     * cos配置
     */
    private Cos cos = new Cos();

    /**
     * 上传文件扩展
     */
    private Extension extension = new Extension();


    @Data
    public static class Cos {

        /**
         * 端点
         */
        private String endpoint;

        /**
         * 访问标识
         */
        private String accessKey;

        /**
         * 密钥
         */
        private String secretKey;

        /**
         * 地区
         */
        private String region;

    }

    @Data
    public static class Oss {

        /**
         * 端点
         */
        private String endpoint;

        /**
         * 访问标识
         */
        private String accessKey;

        /**
         * 密钥
         */
        private String secretKey;
    }

    @Data
    public static class Local {

        /**
         * 上传地址
         */
        private String path;
    }

    @Data
    public static class Minio {
        /**
         * minio地址
         */
        private String url;

        /**
         * 访问标识
         */
        private String accessKey;

        /**
         * 密钥
         */
        private String secretKey;
    }

    @Data
    public static class Extension {
        /**
         * 启用后缀检测
         */
        private boolean enabled = false;

        /**
         * 禁止的后缀
         */
        private String disable;
    }

}
