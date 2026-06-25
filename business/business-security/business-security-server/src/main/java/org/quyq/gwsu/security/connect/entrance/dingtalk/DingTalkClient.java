package org.quyq.gwsu.security.connect.entrance.dingtalk;


import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.callback.DingTalkStreamTopics;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.dingtalk.open.app.stream.protocol.event.EventAckStatus;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.connect.domain.EntranceConfig;
import org.quyq.gwsu.security.connect.enums.EntranceType;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
@Component
@RequiredArgsConstructor
public class DingTalkClient {

    private final DingTalkTextConsumer textConsumer;

    public static final String ASSISTANT_REMOTE_CONTROL_CONFIG_KEY = "assistant_remote_control_config";

    private OpenDingTalkClient client = null;

    private static EntranceConfig.DingTalk CONFIG = null;


    public static EntranceConfig.DingTalk getDingTalkConfig() {
        Optional<EntranceConfig.DingTalk> dingTalkConfig = Optional.ofNullable(CONFIG);
        if (dingTalkConfig.isPresent()) {
            return dingTalkConfig.get();
        }

        throw new BusinessException(SecurityErrorCode.E07009);
    }

    public synchronized void init() throws Exception {

        EntranceConfig config = ConfigInfoUtils.getByObject(ASSISTANT_REMOTE_CONTROL_CONFIG_KEY, EntranceConfig.class);

        if (Objects.nonNull(client)) {
            client.stop();
            CONFIG = null;
        }

        if (EntranceType.DING_TALK != config.getType()) {
            return;
        }

        String clientId = config.getDingTalk().getClientId();
        String clientSecret = config.getDingTalk().getClientSecret();

        AssertUtils.hasText(clientId, SecurityErrorCode.E07007);
        AssertUtils.hasText(clientSecret, SecurityErrorCode.E07008);

        CONFIG = config.getDingTalk();

        client = OpenDingTalkStreamClientBuilder
                .custom()
                .credential(new AuthClientCredential(clientId, clientSecret))
                .registerAllEventListener(event -> {
                    try {
                        //事件唯一Id
                        String eventId = event.getEventId();
                        //事件类型
                        String eventType = event.getEventType();
                        //事件产生时间
                        Long bornTime = event.getEventBornTime();

                        //消费成功
                        return EventAckStatus.SUCCESS;
                    } catch (Exception e) {
                        //消费失败
                        return EventAckStatus.LATER;
                    }
                })
                .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC, textConsumer)
                .build();


        client.start();


    }


}
