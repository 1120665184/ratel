package org.quyq.gwsu.security.connect.entrance.dingtalk.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * @author Quyq
 * @date 2025/12/30
 * @description
 */
@Data
public class CardCallbackResponse {

    //卡片公有数据
    private CardDataDTO cardData;

    //触发回调用户的私有数据
    private CardDataDTO userPrivateData;

    @Data
    @Builder
    public static class CardDataDTO {

        //卡片参数
        private Map<String, String> cardParamMap;
    }
}
