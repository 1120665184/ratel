package org.quyq.gwsu.security.connect.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.security.connect.enums.EntranceType;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description 远程操作对接配置
 */
@Data
public class EntranceConfig {

    @Schema(description = "类型")
    private EntranceType type;

    @Schema(description = "钉钉相关配置")
    private DingTalk dingTalk = new DingTalk();


    @Data
    public static class DingTalk {

        @Schema(description = "协议")
        private String protocol = "https";

        @Schema(description = "区域")
        private String regionId = "central";

        @Schema(description = "端点")
        private String endpoint = "api.dingtalk.com";

        @Schema(description = "Client ID")
        private String clientId;

        @Schema(description = "Client Secret")
        private String clientSecret;

        @Schema(description = "ai输出卡片模板ID")
        private String aiCardTemplateId = "68ea56be-c3a1-4f47-8575-065572e2a601.schema";


        public String apiDomain(){
            return "%s://%s".formatted(protocol, endpoint);
        }

    }

}
