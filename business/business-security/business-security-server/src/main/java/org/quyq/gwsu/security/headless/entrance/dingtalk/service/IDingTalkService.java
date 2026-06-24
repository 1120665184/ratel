package org.quyq.gwsu.security.headless.entrance.dingtalk.service;


import com.dingtalk.open.app.api.models.bot.ChatbotMessage;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
public interface IDingTalkService {

    /**
     * 智能体调用
     *
     * @param chatbotMessage
     */
    void call(ChatbotMessage chatbotMessage);

}
