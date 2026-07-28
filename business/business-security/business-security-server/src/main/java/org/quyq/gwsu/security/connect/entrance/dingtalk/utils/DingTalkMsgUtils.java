package org.quyq.gwsu.security.connect.entrance.dingtalk.utils;

import cn.hutool.core.collection.CollUtil;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.request.OapiV2UserGetRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.dingtalk.api.response.OapiV2UserGetResponse;
import com.taobao.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.security.connect.domain.EntranceConfig;
import org.quyq.gwsu.security.connect.entrance.dingtalk.DingTalkClient;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.DingTalkMessage;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.DingTalkUser;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.PrivateChatResponse;
import org.quyq.gwsu.security.connect.entrance.dingtalk.enums.MsgSourceType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2025/12/19
 * @description 消息数据格式：https://open.dingtalk.com/document/dingstart/types-of-messages-sent-by-robots#9c212e87342hn
 */
@Component
@RequiredArgsConstructor
public class DingTalkMsgUtils {

    private final DingtalkAccessTokenUtils accessTokenUtils;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * 和指定用户私聊
     * 消息数据格式：https://open.dingtalk.com/document/dingstart/types-of-messages-sent-by-robots#9c212e87342hn
     *
     * @param userIds
     * @return
     */
    public PrivateChatResponse toMessages(List<String> userIds, DingTalkMessage message) {
        EntranceConfig.DingTalk config = DingTalkClient.getDingTalkConfig();

        try {
            return restClient.post()
                    .uri(config.apiDomain() + "/v1.0/robot/oToMessages/batchSend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-acs-dingtalk-access-token", accessTokenUtils.getAccessToken())
                    .body(Map.of(
                            "robotCode", config.getClientId(),
                            "userIds", userIds,
                            "msgKey", message.getType(),
                            "msgParam", message.getContent()
                    ))
                    .retrieve()
                    .body(PrivateChatResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 机器人发送群消息（接口暂不支持@用户）
     *
     * @param groupConversationId
     * @param message
     * @return
     */
    public String toGroupMessage(String groupConversationId, DingTalkMessage message) {
        EntranceConfig.DingTalk config = DingTalkClient.getDingTalkConfig();


        try {
            GroupMessageRes body = restClient.post()
                    .uri(config.apiDomain() + "/v1.0/robot/groupMessages/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-acs-dingtalk-access-token", accessTokenUtils.getAccessToken())
                    .body(Map.of(
                            "robotCode", config.getClientId(),
                            "openConversationId", groupConversationId,
                            "msgKey", message.getType(),
                            "msgParam", message.getContent()
                    ))
                    .retrieve()
                    .body(GroupMessageRes.class);
            return Optional.ofNullable(body).map(v -> v.processQueryKey).orElse(null);
        } catch (RestClientException ex) {
            throw new BusinessException(ex);
        }

    }

    record GroupMessageRes(String processQueryKey) {
    }

    /**
     * 发送群文本，支持@用户
     *
     * @param sessionWebhook
     * @param userIds
     * @param mess
     */
    public void toGroupMessageText(String sessionWebhook, List<String> userIds, String mess) {
        com.dingtalk.api.DingTalkClient client = new DefaultDingTalkClient(sessionWebhook);
        try {
            OapiRobotSendRequest request = new OapiRobotSendRequest();
            request.setMsgtype("text");
            OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
            if (CollUtil.isNotEmpty(userIds)) {
                mess = " %s %n ".formatted(userIds.stream().map(v -> "@" + v).collect(Collectors.joining(" "))) + mess;
            }

            text.setContent(mess);
            request.setText(text);
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();

            at.setAtUserIds(userIds);
//           isAtAll类型如果不为Boolean，请升级至最新SDK
            at.setIsAtAll(false);
            request.setAt(at);
            OapiRobotSendResponse response = client.execute(request);
            System.out.println(response.getBody());
        } catch (ApiException e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 消息撤回
     *
     * @param sourceType
     * @param conversationId
     * @param processQueryKeys
     */
    public void revoke(MsgSourceType sourceType, String conversationId, Set<String> processQueryKeys) {
        EntranceConfig.DingTalk config = DingTalkClient.getDingTalkConfig();

        HashMap<String, Object> params = new HashMap<>();

        params.put("robotCode", config.getClientId());
        if (MsgSourceType.GROUP == sourceType) {
            params.put("openConversationId", conversationId);
        }
        params.put("processQueryKeys", processQueryKeys);
        String url = "/v1.0/robot/groupMessages/recall";
        if (MsgSourceType.PRIVATE_CHAT == sourceType) {
            url = "/v1.0/robot/otoMessages/batchRecall";
        }

        try {
            restClient
                    .post()
                    .uri(config.apiDomain() + url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-acs-dingtalk-access-token", accessTokenUtils.getAccessToken())
                    .body(params)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            throw new BusinessException(ex);
        }

    }


    /**
     * 获取文件临时下载URL
     *
     * @param downloadCode
     * @return
     */
    public String getFileDownloadUrl(String downloadCode) {

        EntranceConfig.DingTalk config = DingTalkClient.getDingTalkConfig();

        try {
            FileDownloadResp body = restClient.post()
                    .uri(config.apiDomain() + "/v1.0/robot/messageFiles/download")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-acs-dingtalk-access-token", accessTokenUtils.getAccessToken())
                    .body(Map.of(
                            "robotCode", config.getClientId(),
                            "downloadCode", downloadCode
                    ))
                    .retrieve()
                    .body(FileDownloadResp.class);
            return Optional.ofNullable(body).map(FileDownloadResp::downloadUrl).orElse(null);
        } catch (RestClientException ex) {
            throw new BusinessException(ex);
        }


    }

    record FileDownloadResp(String downloadUrl) {
    }

    /**
     * 获取钉钉的用户详情
     * 文档：https://open.dingtalk.com/document/development/query-user-details?spm=dd_developers.homepage.0.0.31034a97ooHoqY
     *
     * @param userId
     * @return
     */
    public DingTalkUser getDingUserInfo(String userId) {
        DingTalkUser user = new DingTalkUser();
        com.dingtalk.api.DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/get");
        OapiV2UserGetRequest req = new OapiV2UserGetRequest();
        req.setUserid(userId);
        req.setLanguage("zh_CN");
        try {
            OapiV2UserGetResponse rsp = client.execute(req, accessTokenUtils.getAccessToken());
            Long errcode = rsp.getErrcode();
            if (0 != errcode) {
                throw new RuntimeException("获取用户信息异常，消息：%s".formatted(rsp.getErrmsg()));
            }
            OapiV2UserGetResponse.UserGetResponse result = rsp.getResult();
            user.setId(userId);
            user.setUnionId(result.getUnionid());
            user.setName(result.getName());
            user.setMobile(result.getMobile());
            user.setPosition(result.getTitle());
            user.setEmail(result.getEmail());
            user.setWorkPlace(result.getWorkPlace());
            user.setExtension(result.getExtension());
            user.setSenior(result.getSenior());
            user.setAdmin(result.getAdmin());
            user.setBoss(result.getBoss());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        return user;
    }


}