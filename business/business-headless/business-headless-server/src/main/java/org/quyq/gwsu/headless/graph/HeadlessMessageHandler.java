package org.quyq.gwsu.headless.graph;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.quyq.gwsu.common.ai.utils.AgentScopeMessageUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.config.properties.universal.BaseProjectInfoProperties;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.core.HeadlessAgentListener;
import org.quyq.gwsu.headless.core.session.HeadlessPageWrapper;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.util.List;
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

    private final Gson gson = new Gson();

    private final String userId;

    private final AgentStateStore agentStateStore;

    private HeadlessAgentStatus status = HeadlessAgentStatus.INITING;


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
    public void onTextMessageEnd(AguiEvent.TextMessageEnd event, HeadlessPageWrapper wrapper) {
        status = HeadlessAgentStatus.OUTPUTTING;
        sink.tryEmitNext(getContent("\n\r"));
    }

    @Override
    public void onTextMessageContent(String delta, HeadlessPageWrapper wrapper) {
        status = HeadlessAgentStatus.OUTPUTTING;
        // log.info("内容输出：{}", delta);
        sink.tryEmitNext(getContent(delta));

    }

    @Override
    public void onToolCallStart(AguiEvent.ToolCallStart event, HeadlessPageWrapper wrapper) {
        status = HeadlessAgentStatus.CALLING;
        sink.tryEmitNext(getContent(""));
    }

    @Override
    public void onEvent(AguiEvent event, HeadlessPageWrapper wrapper) {
        if (event instanceof AguiEvent.ReasoningMessageContent e) {
            status = HeadlessAgentStatus.THINKING;
            sink.tryEmitNext(getReasoning(e.delta()));
        } else if (event instanceof AguiEvent.Raw e) {
            sink.tryEmitError(new BusinessException(e.rawEvent().toString()));
        }
    }


    //人工审核流处理
    @Override
    public void onHumanApproval(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {
        String threadId = event.getThreadId();
        HumanApprovalInfo approvalInfo = gson.fromJson(gson.toJson(event.value()), new TypeToken<HumanApprovalInfo>() {
        }.getType());

        String tip = approvalInfo.stage() == ApprovalStage.POST_REASONING ? approvalInfo.reasoningStageInfo().getFirst().tip() : approvalInfo.actingStageInfo().tip();

        Msg msg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("\n\r" + tip)
                .build();

        sink.tryEmitNext(getContent(msg, null));

        //录像
        File file = wrapper.stopRecording();
        KitFileInfoVO upload = wrapper.upload(file);

        Msg sp = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("\n\r危险操作需要您审批，以下是操作记录，请确认是否操作：\n")
                .build();
        sink.tryEmitNext(getContent(sp, upload));

        //记录图记忆
        agentStateStore.save(userId, threadId, IntentRecognitionNode.HEADLESS_RECOGNITION_NODE_KEY, List.of(msg));


    }

    //问用户问题处理
    @Override
    public void onAskUserQuestion(String threadId, String toolCallId, List<AskUserQuestionTool.QuestionParam> questions, HeadlessPageWrapper wrapper) {
        StringBuilder sb = new StringBuilder("\n\r# 请您回答以下几个问题：\n\r");
        for (int i = 0; i < questions.size(); i++) {
            AskUserQuestionTool.QuestionParam question = questions.get(i);
            StringBuilder qStr = new StringBuilder();
            qStr.append("## ").append(i + 1).append(".").append(question.question())
                    .append("【").append(question.multiSelect() ? "多选" : "单选").append("】\n\r");
            for (int j = 0; j < question.options().size(); j++) {
                AskUserQuestionTool.QuestionOption option = question.options().get(j);
                qStr.append("* 选项").append(j + 1).append(". ").append(option.label()).append("(")
                        .append(option.description()).append(")\n\r");
            }
            qStr.append("* 选项").append(question.options().size() + 1).append(". ").append("其他，描述您的想法\n");
            sb.append(qStr).append("\n");

        }
        Msg msg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent(sb.toString())
                .build();

        sink.tryEmitNext(getContent(msg, null));
        //记录图记忆
        agentStateStore.save(userId, threadId, IntentRecognitionNode.HEADLESS_RECOGNITION_NODE_KEY, List.of(msg));

    }


    @Override
    public void onAgentOutput(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {
        if ("AGENT_OUTPUT".equals(event.name()) && status != HeadlessAgentStatus.SHOWING) {
            status = HeadlessAgentStatus.SHOWING;
            sink.tryEmitNext(getContent(""));
        }
        //AI输出面板内容输出完成，截取AI输出区截图
        if ("AGENT_OUTPUT_END".equals(event.name())) {
            File file = wrapper.screenshot("#ai-output-panel");
            if (file == null) {
                log.warn("AI输出区截图失败，未获取到截图文件");
                return;
            }
            //               byte[] screenshot = FileUtils.readFileToByteArray(file);
            KitFileInfoVO fileInfo = wrapper.upload(file);
            Msg msg = Msg.builder()
                    .role(MsgRole.ASSISTANT)
                    .textContent("\n以下是助手为您输出的内容：\n")
                    .build();
            sink.tryEmitNext(getContent(msg, fileInfo));
        }
    }

    @Override
    public void onError(Throwable error, HeadlessPageWrapper wrapper) {
        sink.tryEmitError(error);
    }

    public Flux<ChatResponse> asFlux() {
        return sink.asFlux();
    }


    public void complete() {
        status = HeadlessAgentStatus.COMPLETE;
        sink.tryEmitNext(getContent(""));
        sink.tryEmitComplete();
    }

    public void error(Throwable error) {
        status = HeadlessAgentStatus.ERROR;
        sink.tryEmitError(error);
    }

    private ChatResponse getContent(Msg msg, KitFileInfoVO fileInfo) {
        AssistantMessage m = AgentScopeMessageUtils.toAssistantMessage(msg);
        m.getMetadata().put("status", status);
        if (Objects.nonNull(fileInfo)) {
            BaseProjectInfoProperties baseProjectInfoProperties = ConfigInfoUtils.getByObject(BaseProjectInfoProperties.CONFIG_KEY, BaseProjectInfoProperties.class);
            String fileDomain = baseProjectInfoProperties.apiBaseUrl();
            AssistantMessage message = AssistantMessage.builder()
                    .properties(m.getMetadata())
                    .content(m.getText())
                    .toolCalls(m.getToolCalls())
                    .media(List.of(Media.builder()
                            .data("%s/kit/file/stream/%s".formatted(fileDomain, fileInfo.getFileId()))
                            .mimeType(MimeType.valueOf(fileInfo.getMediaType()))
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
        message.getMetadata().put("status", status);
        return new ChatResponse(List.of(new Generation(message)));
    }


    private ChatResponse getReasoning(String reasoning) {
        AssistantMessage message = AgentScopeMessageUtils.toAssistantMessage(Msg.builder()
                .role(MsgRole.ASSISTANT)
                .content(ThinkingBlock.builder()
                        .thinking(reasoning)
                        .build())
                .build());
        message.getMetadata().put("status", status);
        return new ChatResponse(List.of(new Generation(message)));
    }


}
