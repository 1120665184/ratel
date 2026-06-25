package org.quyq.gwsu.security.connect.entrance.dingtalk.utils;


import cn.hutool.core.collection.CollUtil;
import com.aliyun.dingtalkcard_1_0.Client;
import com.aliyun.dingtalkcard_1_0.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.Common;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.connect.domain.EntranceConfig;
import org.quyq.gwsu.security.connect.entrance.dingtalk.DingTalkClient;
import org.quyq.gwsu.security.connect.enums.MsgSourceType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class DingTalkCardUtils {

    private Client client;


    private final DingtalkAccessTokenUtils accessTokenUtils;


    /**
     * 发送卡片
     * @param outTrackId
     * @param sourceType
     * @param subjectId 群组时是conversationId ,私聊时是staffId
     * @param dataMap
     * @return
     */
    public Set<String> sendCard(String outTrackId , MsgSourceType sourceType, String subjectId , Map<String, String> dataMap){
        EntranceConfig.DingTalk dtConfig = DingTalkClient.getDingTalkConfig();

        AssertUtils.hasText(dtConfig.getAiCardTemplateId() , SecurityErrorCode.E07011);

        Client client = createClient();

        dataMap = new HashMap<>(dataMap);
        dataMap.put("outTrackId", outTrackId);

        try {
            CreateAndDeliverHeaders headers
                    = new CreateAndDeliverHeaders();
            headers.xAcsDingtalkAccessToken = accessTokenUtils.getAccessToken();

            CreateAndDeliverRequest.CreateAndDeliverRequestCardData cardData =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestCardData();
            cardData.setCardParamMap(dataMap);

            CreateAndDeliverRequest.CreateAndDeliverRequestImGroupOpenDeliverModel imGroupOpenDeliverModel =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestImGroupOpenDeliverModel()
                            .setRobotCode(dtConfig.getClientId());

            CreateAndDeliverRequest.CreateAndDeliverRequestImGroupOpenSpaceModel imGroupOpenSpaceModel =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestImGroupOpenSpaceModel()
                            .setSupportForward(true);

            CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenDeliverModel imOpenDeliverModel =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenDeliverModel()
                            .setSpaceType("IM_ROBOT")
                            .setRobotCode(dtConfig.getClientId());

            CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenSpaceModel imOpenSpaceModel =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenSpaceModel()
                            .setSupportForward(true);

            CreateAndDeliverRequest request
                    = new CreateAndDeliverRequest()
                    .setOutTrackId(outTrackId)
                    .setCardTemplateId(dtConfig.getAiCardTemplateId())
                    .setCardData(cardData)
                    .setCallbackType("STREAM")
                    .setUserIdType(1);
            if (MsgSourceType.GROUP == sourceType) {
                request.setImGroupOpenSpaceModel(imGroupOpenSpaceModel)
                        .setImGroupOpenDeliverModel(imGroupOpenDeliverModel)
                        .setOpenSpaceId(getGroupOpenSpaceId(subjectId));
            } else {
                request.setImRobotOpenSpaceModel(imOpenSpaceModel)
                        .setImRobotOpenDeliverModel(imOpenDeliverModel)
                        .setOpenSpaceId(getRobotOpenSpaceId(subjectId))
                        .setUserId(subjectId);
            }

            CreateAndDeliverResponse resp = client.createAndDeliverWithOptions(request, headers,
                    new RuntimeOptions());
            return resp.getBody().result.deliverResults.stream().map(CreateAndDeliverResponseBody.CreateAndDeliverResponseBodyResultDeliverResults::getCarrierId)
                    .collect(Collectors.toSet());
        }catch (Exception e){
            throw new BusinessException(e);
        }


    }


    /**
     * 更新卡片数据
     * @param outTrackId
     * @param data
     */
    public void updateCard(String outTrackId, Map<String, String> data) {
        updateCard(outTrackId, data, null);
    }
    public void updateCard(String outTrackId, Map<String, String> data, Map<String, PrivateDataValue> privateData) {
        Client client = createClient();
        UpdateCardHeaders updateCardHeaders
                = new UpdateCardHeaders();
        updateCardHeaders.xAcsDingtalkAccessToken = accessTokenUtils.getAccessToken();
        UpdateCardRequest.UpdateCardRequestCardUpdateOptions cardUpdateOptions
                = new UpdateCardRequest.UpdateCardRequestCardUpdateOptions()
                .setUpdateCardDataByKey(true);


        UpdateCardRequest updateCardRequest
                = new UpdateCardRequest()
                .setOutTrackId(outTrackId)
                .setCardUpdateOptions(cardUpdateOptions)
                .setUserIdType(1);
        if (CollUtil.isNotEmpty(privateData)) {
            updateCardRequest.setPrivateData(privateData);
        }
        if (CollUtil.isNotEmpty(data)) {
            UpdateCardRequest.UpdateCardRequestCardData cardData
                    = new UpdateCardRequest.UpdateCardRequestCardData()
                    .setCardParamMap(data);

            updateCardRequest.setCardData(cardData);
        }
        try {
            client.updateCardWithOptions(updateCardRequest, updateCardHeaders,
                    new RuntimeOptions());
        } catch (TeaException err) {
            if (!Common.empty(err.code) && !Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
                log.error("CardManager#updateCard get TeaException, msg:{} ", err.message);
            }

        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!Common.empty(err.code) && !Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
                log.error("CardManager#updateCard get Exception, msg:{} ", err.message);
            }

        }

    }


    public void streamAiCard(String outTrackId, String key ,String content , boolean finish) {
        Client client = createClient();
        try {
            StreamingUpdateHeaders headers = new StreamingUpdateHeaders();
            headers.xAcsDingtalkAccessToken = accessTokenUtils.getAccessToken();
            StreamingUpdateRequest request =
                    new StreamingUpdateRequest()
                            .setOutTrackId(outTrackId)
                            .setGuid(UUID.randomUUID().toString())
                            .setKey(key)
                            .setContent(content)
                            .setIsFull(true)
                            .setIsFinalize(finish);
            client.streamingUpdateWithOptions(request, headers, new RuntimeOptions());
        } catch (Exception e) {
            log.error("CardManager#finishAiCard get exception = " + e);
        }
    }




    protected Client createClient() {

        if(Objects.nonNull(client)) {
            return client;
        }

        synchronized (DingTalkCardUtils.class) {
            if(Objects.nonNull(client)) {
                return client;
            }

            EntranceConfig.DingTalk dtConfig = DingTalkClient.getDingTalkConfig();

            try {
                Config config = new Config();
                config.protocol = dtConfig.getProtocol();
                config.regionId = dtConfig.getRegionId();
                config.endpoint = dtConfig.getEndpoint();
                client =  new Client(config);
            } catch (Exception e) {
                log.error("createClient get excpetion, msg:{}", e.getMessage());
            }

        }


        return client;
    }


    protected String getGroupOpenSpaceId(String openConvId) {
        return "dtv1.card//IM_GROUP." + openConvId;
    }

    protected String getRobotOpenSpaceId(String userId) {
        return "dtv1.card//im_robot." + userId;
    }


}
