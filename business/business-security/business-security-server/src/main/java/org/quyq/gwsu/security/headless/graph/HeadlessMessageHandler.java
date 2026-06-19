package org.quyq.gwsu.security.headless.graph;


import cn.hutool.core.util.ArrayUtil;
import com.alibaba.cloud.ai.agent.agentscope.AgentScopeMessageUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.quyq.gwsu.common.ai.session.CommonSessionKey;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.security.headless.HeadlessAgentListener;
import org.quyq.gwsu.security.headless.session.HeadlessPageWrapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
@Slf4j
@RequiredArgsConstructor
public class HeadlessMessageHandler implements HeadlessAgentListener {

    private final Sinks.Many<ChatResponse> sink = Sinks.many().multicast().onBackpressureBuffer();

    Gson gson = new Gson();

    private final String userId;

    private final Session session;


    @Override
    public void onRunStarted(AguiEvent.RunStarted event, HeadlessPageWrapper wrapper) {
        //开始录像
        wrapper.startRecording();
    }

    @Override
    public void onRunFinished(AguiEvent.RunFinished event, HeadlessPageWrapper wrapper) {
        //停止录像
        wrapper.discardRecording();
    }

    @Override
    public void onTextMessageContent(String delta, HeadlessPageWrapper wrapper) {
       // log.info("内容输出：{}", delta);
        sink.tryEmitNext(getContent(delta));

    }


    @Override
    public void onEvent(AguiEvent event, HeadlessPageWrapper wrapper) {
        if (event instanceof AguiEvent.ReasoningMessageContent e) {
            sink.tryEmitNext(getReasoning(e.delta()));
        }
    }

    //人工审核流处理
    @Override
    public void onHumanApproval(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {
        File file = null;
        String threadId = event.getThreadId();
        HumanApprovalInfo approvalInfo = gson.fromJson(gson.toJson(event.value()) , new TypeToken<HumanApprovalInfo>() {}.getType());

        String tip = approvalInfo.stage() == ApprovalStage.POST_REASONING ? approvalInfo.reasoningStageInfo().getFirst().tip() : approvalInfo.actingStageInfo().tip();

        Msg msg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent(tip)
                .build();

        sink.tryEmitNext(getContent(msg , null , null));

        try {
            //录像
            byte[] record = FileUtils.readFileToByteArray(file = wrapper.stopRecording());
            Msg sp = Msg.builder()
                    .role(MsgRole.ASSISTANT)
                    .textContent("\n以下是操作记录：\n")
                    .build();
            sink.tryEmitNext(getContent(sp , record , MimeType.valueOf("video/mp4")));
        } catch (IOException e) {
            log.error("操作记录发送失败" , e);
        } finally {
            if(Objects.nonNull(file)){
                file.delete();
            }
        }

        //记录图记忆
        session.save(CommonSessionKey.of(threadId , userId) ,IntentRecognitionNode.HEADLESS_RECOGNITION_NODE_KEY , List.of(msg));


    }

    //问用户问题处理
    @Override
    public void onAskUserQuestion(String threadId, String toolCallId, List<AskUserQuestionTool.QuestionParam> questions, HeadlessPageWrapper wrapper) {
        StringBuilder sb = new StringBuilder("请您回答以下几个问题：\n");
        for (int i = 0 ; i < questions.size(); i++) {
            AskUserQuestionTool.QuestionParam question = questions.get(i);
            StringBuilder qStr = new StringBuilder();
            qStr.append(i + 1).append(".").append(question.question())
                    .append("【").append(question.multiSelect() ? "多选" : "单选").append("】\n");
            for (int j = 0 ; j < question.options().size(); j++) {
                AskUserQuestionTool.QuestionOption option = question.options().get(j);
                qStr.append("  选项").append(j + 1).append(". ").append(option.label()).append("(")
                        .append(option.description()).append(")\n");
            }
            qStr.append("  选项").append(question.options().size() + 1).append(". ").append("其他，描述您的想法\n");
            sb.append(qStr).append("\n");

        }
        Msg msg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent(sb.toString())
                .build();

        sink.tryEmitNext(getContent(msg , null , null));
        //记录图记忆
        session.save(CommonSessionKey.of(threadId , userId) ,IntentRecognitionNode.HEADLESS_RECOGNITION_NODE_KEY , List.of(msg));

    }

    @Override
    public void onError(Throwable error, HeadlessPageWrapper wrapper) {
        sink.tryEmitError(error);
    }

    public Flux<ChatResponse> asFlux() {
        return sink.asFlux();
    }


    public void complete() {
        sink.tryEmitComplete();
    }



    private ChatResponse getContent(Msg msg , byte[] resource , MimeType mimeType) {
        AssistantMessage m = AgentScopeMessageUtils.toAssistantMessage(msg);
        if(ArrayUtil.isNotEmpty(resource)){
            AssistantMessage message = AssistantMessage.builder()
                    .properties(m.getMetadata())
                    .content(m.getText())
                    .toolCalls(m.getToolCalls())
                    .media(List.of(Media.builder()
                            .data(resource)
                            .mimeType(mimeType)
                            .build()))
                    .build();
            return new ChatResponse(List.of(new Generation(message)));
        }


        return new ChatResponse(List.of(new Generation(m)));
    }

    private ChatResponse getContent(String delta) {

        AssistantMessage message = AgentScopeMessageUtils.toAssistantMessage(Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent(delta)
                .build());

        return new ChatResponse(List.of(new Generation(message)));
    }

    private ChatResponse getReasoning(String reasoning) {
        AssistantMessage message = AgentScopeMessageUtils.toAssistantMessage(Msg.builder()
                .role(MsgRole.ASSISTANT)
                .content(ThinkingBlock.builder()
                        .thinking(reasoning)
                        .build())
                .build());
        return new ChatResponse(List.of(new Generation(message)));
    }



}
