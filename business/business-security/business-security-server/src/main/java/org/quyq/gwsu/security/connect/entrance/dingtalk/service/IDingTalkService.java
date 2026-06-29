package org.quyq.gwsu.security.connect.entrance.dingtalk.service;


import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import org.quyq.gwsu.security.connect.entrance.dingtalk.vo.UserStaffIdMappingInfo;

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


    UserStaffIdMappingInfo getUserMappingInfo(String staffId);

}
