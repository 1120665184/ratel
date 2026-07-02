package org.quyq.gwsu.security.connect.entrance.dingtalk;


import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.headless.api.HeadlessClientApi;
import org.quyq.gwsu.headless.api.dto.NewChatDTO;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.CardCallbackRequest;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.CardCallbackResponse;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.DingTalkMessage;
import org.quyq.gwsu.security.connect.entrance.dingtalk.service.IDingTalkService;
import org.quyq.gwsu.security.connect.entrance.dingtalk.utils.DingTalkCardUtils;
import org.quyq.gwsu.security.connect.entrance.dingtalk.utils.DingTalkMsgUtils;
import org.quyq.gwsu.security.connect.entrance.dingtalk.vo.UserStaffIdMappingInfo;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/6/29
 * @description
 */
@Component
@RequiredArgsConstructor
public class DingTalkCardConsumer implements OpenDingTalkCallbackListener<CardCallbackRequest, CardCallbackResponse> {

    private final ObjectMapper objectMapper;

    private final HeadlessClientApi headlessClientApi;

    private final IDingTalkService dingTalkService;

    private final DingTalkMsgUtils dingTalkMsgUtils;

    private final DingTalkCardUtils dingTalkCardUtils;

    @Override
    public CardCallbackResponse execute(CardCallbackRequest cardCallbackRequest) {
        CardCallbackRequest.ActionCallbackContent content = objectMapper.readValue(cardCallbackRequest.getContent(), CardCallbackRequest.ActionCallbackContent.class);
        List<String> actionIds = content.getCardPrivateData().getActionIds();
        String userId = cardCallbackRequest.getUserId();
        //开启新会话
        if (actionIds.contains("newChat")) {
            return newChat(userId, cardCallbackRequest.getOutTrackId());
        }


        return null;
    }

    private CardCallbackResponse newChat(String userId, String outTrackId) {
        UserStaffIdMappingInfo userMappingInfo = dingTalkService.getUserMappingInfo(userId);
        NewChatDTO dto = new NewChatDTO();
        dto.setSign(IDingTalkService.SIGN);
        dto.setUserId(userMappingInfo.getSubjectId());
        FeignUtils.data(headlessClientApi.newThreadId(dto));
        dingTalkMsgUtils.toMessages(List.of(userId), DingTalkMessage.sampleText()
                .content("您已开启新会话，有什么问题需要问我吗～～")
                .build());
        dingTalkCardUtils.updateCard(outTrackId, Map.of("newChat", "false"));

        return new CardCallbackResponse();
    }

}
