package org.quyq.gwsu.security.connect.entrance.dingtalk.service.impl;


import cn.hutool.core.util.IdUtil;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import com.dingtalk.open.app.api.models.bot.MessageContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.api.IAccountInfoClientApi;
import org.quyq.gwsu.headless.api.HeadlessClientApi;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.dto.ImageBlock;
import org.quyq.gwsu.headless.api.dto.UserMsg;
import org.quyq.gwsu.headless.api.dto.VideoBlock;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.api.vo.AssistantMsg;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.DingTalkMessage;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.DingTalkUser;
import org.quyq.gwsu.security.connect.entrance.dingtalk.service.IDingTalkService;
import org.quyq.gwsu.security.connect.entrance.dingtalk.utils.DingTalkCardUtils;
import org.quyq.gwsu.security.connect.entrance.dingtalk.utils.DingTalkMsgUtils;
import org.quyq.gwsu.security.connect.entrance.dingtalk.vo.UserStaffIdMappingInfo;
import org.quyq.gwsu.security.connect.enums.DingTalkMsgType;
import org.quyq.gwsu.security.connect.enums.MsgSourceType;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkServiceImpl implements IDingTalkService {

    private final DingTalkMsgUtils dingTalkMsgUtils;

    private final DingTalkCardUtils dingTalkCardUtils;

    private final CacheUtils cacheUtils;

    private final IAccountInfoClientApi accountInfoClientApi;

    private final HeadlessClientApi headlessClientApi;

    private final ObjectMapper objectMapper;

    private final static String DING_TALK_USER_MAPPING = "dingtalk:user_mapping:";

    private final String errMsg = "出错啦，请联系管理员";

    @Override
    public void call(ChatbotMessage chatbotMessage) {
        String staffId = chatbotMessage.getSenderStaffId();

        try {
            UserStaffIdMappingInfo mappingInfo = getUserMappingInfo(staffId);

            if (!StringUtils.hasText(mappingInfo.getSubjectId())) {
                dingTalkMsgUtils.toMessages(Collections.singletonList(staffId), DingTalkMessage.sampleText()
                        .content("⚠️该账号暂时未和Ratel系统绑定 ，请到管理系统系统绑定或联系管理员😊")
                        .build());
                return;
            }
            agentCall(MsgSourceType.getMsgSourceType(chatbotMessage.getConversationType()), mappingInfo, getContent(chatbotMessage));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dingTalkMsgUtils.toMessages(Collections.singletonList(staffId), DingTalkMessage.sampleText()
                    .content("出错啦，请联系管理员")
                    .build());
        }

    }


    protected void agentCall(MsgSourceType sourceType, UserStaffIdMappingInfo mappingInfo, UserContent content) {
        //TODO 目前只支持文本
        if (DingTalkMsgType.TEXT != content.type()) {
            throw new BusinessException("不支持的消息类型");
        }
        String outTrackId = IdUtil.getSnowflakeNextIdStr();

        AtomicReference<HeadlessAgentStatus> status = new AtomicReference<>(HeadlessAgentStatus.CONNECTION);
        String[] errInfo = new String[1];

        AtomicInteger progress = new AtomicInteger(0);
        AtomicReference<Map<String, String>> lastParam = new AtomicReference<>(buildParam(HeadlessAgentStatus.CONNECTION, 0, null, null));

        //发送卡片
        dingTalkCardUtils.sendCard(outTrackId, sourceType, mappingInfo.getStaffId(), lastParam.get());

        AssistantResponse response = new AssistantResponse();
        headlessClientApi.stream(mappingInfo.getSubjectId() ,new HeadlessDTO(
                        UserMsg.ofText(content.content.getContent()),null))
                .doOnNext(chunk -> {
                    if (status.get() == HeadlessAgentStatus.ERROR) {
                        return;
                    }
                    status.set(chunk.status());
                    AssistantMsg message = chunk.message();
                    log.debug("消息: status={}, text={}", status, objectMapper.writeValueAsString(message));

                    // 服务端错误已包装为 ERROR 状态事件，直接处理
                    if (HeadlessAgentStatus.ERROR == status.get() || HeadlessAgentStatus.BUSY == status.get()) {
                        errInfo[0] = chunk.errorMessage();
                        log.error("智能体返回错误: {}", chunk.errorMessage());
                        return;
                    }

                    setProgress(progress, status.get());
                    if (HeadlessAgentStatus.CONNECTION == status.get()) {
                        dingTalkCardUtils.streamAiCard(outTrackId, "content", "loading", false);
                    }
                    String imageUrl = null;
                    String videoUrl = null;
                    List<ImageBlock> images = message.getImageBlocks();
                    List<VideoBlock> videos = message.getVideoBlocks();
                    if (!images.isEmpty()) {
                        imageUrl = images.getFirst().getUrl();
                    }
                    if (!videos.isEmpty()) {
                        videoUrl = videos.getFirst().getUrl();
                    }

                    //更新状态
                    Map<String, String> params = buildParam(status.get(), progress.get(), imageUrl, videoUrl);
                    if (!lastParam.get().equals(params)) {
                        lastParam.set(params);
                        dingTalkCardUtils.updateCard(outTrackId, params);
                    }

                    String text = message.getTextContent();
                    if (StringUtils.hasText(text)) {
                        String tmp = response.cacheContent(text);
                        if (StringUtils.hasText(tmp)) {
                            dingTalkCardUtils.streamAiCard(outTrackId, "content", tmp, false);
                        }
                    }


                })
                .doOnComplete(() -> {
                    HeadlessAgentStatus tmp = status.get();
                    String msg = response.getContent();
                    if(HeadlessAgentStatus.ERROR == tmp) {
                        msg = errMsg;
                    }else if(HeadlessAgentStatus.BUSY == tmp) {
                        msg = errInfo[0];
                    }
                    dingTalkCardUtils.streamAiCard(outTrackId, "content", msg, true);
                })
                .doOnError(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    dingTalkCardUtils.streamAiCard(outTrackId, "content", errMsg, true);
                })
                .subscribe();

    }


    private void setProgress(AtomicInteger progress, HeadlessAgentStatus status) {
        int p = progress.get();
        if (HeadlessAgentStatus.CONNECTION == status) {
            progress.set(0);
        } else if (HeadlessAgentStatus.INITING == status) {
            progress.set(20);
        } else if (HeadlessAgentStatus.THINKING == status && p < 40) {
            progress.set(40);
        } else if (HeadlessAgentStatus.OUTPUTTING == status) {
            if (p < 90) {
                progress.set(p + 5);
            }
        } else if (HeadlessAgentStatus.SHOWING == status) {
            progress.set(90);
        } else if (HeadlessAgentStatus.COMPLETE == status) {
            progress.set(100);
        }
    }

    private Map<String, String> buildParam(HeadlessAgentStatus status, int progress, String imageUrl, String videoUrl) {
        Map<String, String> param = new HashMap<>();
        param.put("status", status.getStatus());
        param.put("image_url", imageUrl);
        param.put("video_url", videoUrl);
        param.put("progress", progress + "");
        return param;
    }


    /**
     * 通过钉钉staffId找到映射的本系统userId
     *
     * @param staffId
     * @return
     */
    protected UserStaffIdMappingInfo getUserMappingInfo(String staffId) {
        UserStaffIdMappingInfo info = cacheUtils.get(DING_TALK_USER_MAPPING + staffId);
        if (Objects.nonNull(info) && StringUtils.hasText(info.getSubjectId())) {
            return info;
        }

        if (Objects.isNull(info) || !StringUtils.hasText(info.getUnionId())) {
            DingTalkUser dingUserInfo = dingTalkMsgUtils.getDingUserInfo(staffId);
            AssertUtils.hasText(dingUserInfo.getUnionId(), SecurityErrorCode.E07010);

            info = new UserStaffIdMappingInfo();
            info.setStaffId(staffId);
            info.setUnionId(dingUserInfo.getUnionId());
        }


        try {
            String userId = FeignUtils.data(accountInfoClientApi.getUserIdByDingTalkUnionId(info.getUnionId()));
            if (!StringUtils.hasText(userId)) {
                return info;
            }
            info.setSubjectId(userId);

            return info;
        } finally {
            cacheUtils.set(DING_TALK_USER_MAPPING + staffId, info, 30, TimeUnit.DAYS);
        }

    }


    protected UserContent getContent(ChatbotMessage message) {
        DingTalkMsgType type = DingTalkMsgType.getByCode(message.getMsgtype());
        return new UserContent(type,
                DingTalkMsgType.TEXT == type ? message.getText() : message.getContent());
    }


    protected record UserContent(
            DingTalkMsgType type,
            MessageContent content) {
    }

    protected static class AssistantResponse {

        private final StringBuilder think = new StringBuilder();

        private final StringBuilder content = new StringBuilder();

        private final int cacheLength = 10;

        private final AtomicInteger thinkLength = new AtomicInteger(0);
        private final AtomicInteger contentLength = new AtomicInteger(0);


        /**
         * 缓存think内容
         *
         * @param delta
         * @return 返回内容时，需要更新到前端
         */
        public String cacheThink(String delta) {
            think.append(delta);
            if (think.length() - thinkLength.get() > cacheLength) {
                thinkLength.set(think.length());
                return think.toString();
            }
            return null;
        }

        /**
         * 缓存输出内容
         *
         * @param delta
         * @return 返回内容时，需要更新到前端
         */
        public String cacheContent(String delta) {
            content.append(delta);
            if (content.length() - contentLength.get() > cacheLength) {
                contentLength.set(content.length());
                return content.toString();
            }
            return null;
        }

        public String getContent() {
            return content.toString();
        }


    }

}
