package org.quyq.gwsu.security.connect.entrance.dingtalk;


import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.DingTalkMessage;
import org.quyq.gwsu.security.connect.entrance.dingtalk.service.IDingTalkService;
import org.quyq.gwsu.security.connect.entrance.dingtalk.utils.DingTalkMsgUtils;
import org.quyq.gwsu.security.connect.entrance.dingtalk.enums.MsgSourceType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DingTalkTextConsumer implements OpenDingTalkCallbackListener<ChatbotMessage, Void> {

    private final ObjectMapper objectMapper;

    private final IDingTalkService dingTalkService;

    private final DingTalkMsgUtils dingTalkMsgUtils;


    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    @Override
    public Void execute(ChatbotMessage chatbotMessage) {

        MsgSourceType conversationType = MsgSourceType.getMsgSourceType(chatbotMessage.getConversationType());
        //忽略群聊
        if(MsgSourceType.GROUP == conversationType){
            dingTalkMsgUtils.toMessages(List.of(chatbotMessage.getSenderStaffId()) ,
                    DingTalkMessage.sampleText()
                            .content("有什么问题可以私聊我哦😊")
                            .build());
            return null;
        }

        executorService.execute(() ->handler(chatbotMessage));

        return null;
    }

    private void handler(ChatbotMessage chatbotMessage){
        log.debug("钉钉消息：{}" , objectMapper.writeValueAsString(chatbotMessage));
        dingTalkService.call(chatbotMessage);

    }

}
