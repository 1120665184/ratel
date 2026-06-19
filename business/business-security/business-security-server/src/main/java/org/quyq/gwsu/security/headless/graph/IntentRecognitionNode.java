package org.quyq.gwsu.security.headless.graph;


import com.alibaba.cloud.ai.agent.agentscope.AgentScopeMessageUtils;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.gson.Gson;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.session.Session;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.session.CommonSessionKey;
import org.quyq.gwsu.security.brain.ModelProvider;
import org.quyq.gwsu.security.constants.SerConstants;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.quyq.gwsu.security.headless.domain.RouterInfo;
import org.quyq.gwsu.security.headless.enums.GraphRouteType;
import org.quyq.gwsu.security.headless.session.HeadlessAccessSession;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 意图识别节点，有意图模糊问题直接询问用户
 */
@RequiredArgsConstructor
public class IntentRecognitionNode implements NodeAction {

    private final Session session;

    private final HeadlessBrowserManager headlessBrowserManager;

    public final static String HEADLESS_RECOGNITION_NODE_KEY = "headless_recognition_node";

    private final Gson gson = new Gson();


    @Override
    public Map<String, Object> apply(OverAllState state) {
        String threadId = (String) state.value(SerConstants.Headless.GRAPH_PARAM_THREAD_ID).orElse("");
        String query = state.value(SerConstants.Headless.GRAPH_PARAM_QUERY, "");
        String userId = state.value(SerConstants.Headless.GRAPH_PARAM_USER_ID, String.class).orElse("");

        if (StringUtils.hasText(threadId)) {
            headlessBrowserManager.newSession(userId, threadId);
        } else {
            threadId = Optional.ofNullable(headlessBrowserManager.getAccessSession(userId))
                    .map(HeadlessAccessSession::threadId)
                    .orElse("");
        }


        ReActAgent agent = buildAgent();

        InMemoryMemory memory = new InMemoryMemory();
        //加载历史记忆
        if (StringUtils.hasText(threadId)) {
            memory.loadIfExists(session, CommonSessionKey.of(threadId, userId));
        }

        List<Msg> messages = memory.getMessages();

        //没有历史消息直接下一步(首次必定是普通chat分支)
        if (CollectionUtils.isEmpty(messages)) {

            return Map.of(SerConstants.Headless.GRAPH_PARAM_THREAD_ID, threadId,
                    SerConstants.Headless.GRAPH_PARAM_ROUTE_INFO, RouterInfo.builder()
                            .type(GraphRouteType.CHAT)
                            .build());
        }

        Msg newMsg = messages.getLast();
        //判断是否为审批
        boolean isApproval = newMsg.getMetadata().containsKey(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY);

        //判断是否为回复AI内容
        boolean isAnswer = newMsg.getRole() == MsgRole.ASSISTANT && newMsg.getContent().stream()
                .anyMatch(v -> (v instanceof ToolUseBlock t) && t.getName().equals(AIConstants.ToolName.ASK_USER_QUESTION));


        String systemContent = "无";

        String userTemplate = """
                <system>%s</system>
                ## 用户回复内容：
                %s
                """;

        if (isApproval) {
            systemContent = "用户消息属于审批回复\n审批提醒内容元数据：" + gson.toJson(newMsg.getMetadata().get(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY));

        } else if (isAnswer) {
            ToolUseBlock toolUseBlock = newMsg.getContent().stream()
                    .filter(v -> (v instanceof ToolUseBlock t) && t.getName().equals(AIConstants.ToolName.ASK_USER_QUESTION))
                    .map(v -> (ToolUseBlock) v).findFirst().orElse(null);
            systemContent = "用户消息属于回答模型提出的问题\n";
            systemContent += """
                    模型提出的问题有关的元数据：
                    toolCallId: %s
                    问题内容：%s
                    """.formatted(toolUseBlock.getId(), gson.toJson(toolUseBlock.getInput()));
        }

        //追加节点会话历史消息
        CommonSessionKey sessionKey = CommonSessionKey.of(threadId, userId);
        List<Msg> nodeHistory = new ArrayList<>();
        if (StringUtils.hasText(threadId)) {
            nodeHistory = session.getList(sessionKey, HEADLESS_RECOGNITION_NODE_KEY, Msg.class);
            if (!CollectionUtils.isEmpty(nodeHistory)) {
                Memory agentMemory = agent.getMemory();
                nodeHistory.forEach(agentMemory::addMessage);
            }
        }

        RouterInfo routerInfo = Optional.ofNullable(
                        agent.call(Msg.builder()
                                        .role(MsgRole.USER)
                                        .textContent(userTemplate.formatted(systemContent, query))
                                        .build(), RouterInfo.class)
                                .block()
                ).map(r -> r.getStructuredData(RouterInfo.class))
                .orElseThrow();


        GraphRouteType type = routerInfo.getType();
        if (type == GraphRouteType.UNKNOWN) {
            //保存该节点的会话记录
            nodeHistory = new ArrayList<>(nodeHistory);
            nodeHistory.add(Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(query)
                    .build());

            Msg assistantMsg = Msg.builder()
                    .role(MsgRole.ASSISTANT)
                    .textContent(routerInfo.getUnknownReply())
                    .build();

            nodeHistory.add(assistantMsg);
            session.save(sessionKey, HEADLESS_RECOGNITION_NODE_KEY, nodeHistory);

            Flux<ChatResponse> output = Flux.just(getContent(assistantMsg));

            //返回AI回复
            return Map.of(SerConstants.Headless.GRAPH_PARAM_THREAD_ID, threadId,
                    SerConstants.Headless.GRAPH_PARAM_OUTPUT, output);
        }


        //清除节点会话
        session.delete(sessionKey, HEADLESS_RECOGNITION_NODE_KEY);

        return Map.of(SerConstants.Headless.GRAPH_PARAM_THREAD_ID, threadId,
                SerConstants.Headless.GRAPH_PARAM_ROUTE_INFO, routerInfo);
    }


    private ReActAgent buildAgent() {

        return ReActAgent.builder()
                .name("intentRecognitionAgent")
                .sysPrompt(sysPrompt())
                .model(ModelProvider.generateModel())
                .memory(new InMemoryMemory())
                .build();

    }


    private ChatResponse getContent(Msg msg) {

        AssistantMessage message = AgentScopeMessageUtils.toAssistantMessage(msg);

        return new ChatResponse(List.of(new Generation(message)));
    }


    private String sysPrompt() {

        return """
                # AI 提示词：意图识别与路由决策
                
                ## 角色
                你是一个专业的对话路由与信息抽取助手。你的任务是根据**用户最新回复**（包含 `<system>` 标签和用户原话）以及**历史会话上下文**，准确判断当前消息应路由到哪个分支，并按指定 JSON 结构提取参数。
                
                ## 输入格式
                你将收到：
                1. **历史会话**（仅用于辅助理解，但路由主要依据 `<system>` 标签）
                2. **用户最新回复**，格式如下：
                ```text
                <system>系统标签内容</system>
                ## 用户回复内容：
                用户实际输入的原话
                ```
                
                `<system>` 内容由上游系统生成，可能包含以下类型（但不限于）：
                - **“无”** 或空：表示无特殊业务意图，属于正常对话。
                - **包含“审批”字样**：表示该消息预期为审批回复，且 `<system>` 中会附带审批相关的元数据（如审批提醒内容）。
                - **包含“回答模型提出的问题”**：表示该消息预期为回答之前模型通过工具 `AskUserQuestion` 提出的问题，且 `<system>` 中会附带 `toolCallId` 和问题列表的 JSON 字符串。
                
                ## 路由类型（共四种）
                - **CHAT**：普通对话（无审批或问答意图）。
                - **APPROVAL**：用户明确同意或拒绝审批请求。
                - **ANSWER**：用户完整回答了模型提出的所有问题。
                - **UNKNOWN**：在上述两种意图上下文中，用户回复不完整或态度模糊，需要引导澄清。
                
                ## 判断规则（按顺序执行）
                
                ### 1. 解析 `<system>` 标签
                - 提取标签内的文本内容。
                - 若内容为“无”或空字符串 → **直接判定为 `CHAT`**，不再执行后续检查。
                - 若内容包含“审批” → 进入 **审批处理流程**。
                - 若内容包含“回答模型提出的问题” → 进入 **问答处理流程**。
                - 若内容有其他文本（理论上不会出现），可视为“无”处理。
                
                ### 2. 审批处理流程（system 含“审批”）
                - **目标**：判断用户是否明确同意或拒绝。
                - **分析用户回复原话**：
                - **明确同意**：包含“同意/批准/可以/好/行/ok/没问题/赞成/是”等肯定词 → 类型 `APPROVAL`，`agree=true`，`refuseReason=""`。
                - **明确拒绝**：包含“拒绝/不同意/不行/不可以/驳回/反对/否”等否定词 → 类型 `APPROVAL`，`agree=false`，并提取拒绝原因（若“因为”之后有内容则取之，否则为空字符串）。
                - **未明确表态**：未包含上述任何明确肯定或否定词（如“我再想想/也许吧/不确定/稍后决定/看看再说/需要时间”等）→ 类型 `UNKNOWN`，需生成 `unknownReply` 引导明确表态。
                - **`UNKNOWN` 回复示例**：`“系统识别到您的消息涉及审批，但您的意图尚不明确。请明确回复‘同意’或‘拒绝’以继续处理。”`
                
                ### 3. 问答处理流程（system 含“回答模型提出的问题”）
                - **目标**：判断用户是否回答了所有问题。
                - **解析 system 中的元数据**：
                - 提取 `toolCallId`（字符串）。
                - 提取问题列表（通常为 JSON 数组，例如 `["问题1", "问题2"]`）。
                - **分析用户回复原话**：
                - 将用户回复与问题列表逐一匹配，判断哪些问题已被回答（通过语义理解，不一定要求完全匹配，只要用户明确给出了对应答案即可）。
                - **若所有问题都有明确答案** → 类型 `ANSWER`，构造 `answerInfo` 映射（问题原文 → 用户回答），并**务必填充 `toolCallId`**（从 system 中获取）。
                - **若部分问题未回答或全部未回答** → 类型 `UNKNOWN`，构造 `answerInfo` 仅包含已回答的问题（可为空），并生成 `unknownReply`，清晰列出尚未回答的问题，引导用户补充。
                - **`UNKNOWN` 回复示例**：`“您还有以下问题未回答：1. 您的居住城市？2. 您的年龄？请补充回答。”`
                - **注意**：若 system 中未提供问题列表或 `toolCallId`，则尝试从历史会话中提取；若仍缺失，可在 `unknownReply` 中说明“未检测到待回答问题，请重新表述”。
                
                ### 4. 其他情况
                - 若 `<system>` 内容既不是“无”也不含上述关键词，则按“无”处理，归为 `CHAT`。
                
                ## 输出 JSON 结构
                你必须输出一个符合以下 `RouterInfo` 定义的 JSON 对象（纯 JSON，无额外文字）。
                
                ```json
                {
                "type": "CHAT" | "APPROVAL" | "ANSWER" | "UNKNOWN",
                "approvalInfo": {
                 "agree": true | false,
                 "refuseReason": "string"
                } | null,
                "answerInfo": {
                 "问题原文1": "用户回答1",
                 "问题原文2": "用户回答2"
                } | null,
                "unknownReply": "string" | null,
                "toolCallId": "string" | null
                }
                ```
                **字段说明：**
                - `type`：必填。
                - `approvalInfo`：仅当 `type=APPROVAL` 时提供对象，否则为 `null`。
                - `answerInfo`：仅当 `type=ANSWER 或 UNKNOWN`（且涉及问答）时提供对象；`ANSWER` 必须包含所有问题，`UNKNOWN` 可仅包含已回答部分。
                - `unknownReply`：仅当 `type=UNKNOWN` 时必须提供字符串，用于引导用户澄清；其他类型为 `null`。
                - `toolCallId`：仅当 `type=ANSWER` 时必须填充，从 `system` 标签中提取；其他类型为 `null`。
                
                ## 注意事项
                1. **优先遵循`<system>`标签的意图**：即使历史会话存在矛盾，也以`<system>`为准（因为它是上游系统根据完整状态生成的）。
                2. **UNKNOWN 仅出现在审批或问答上下文中**：若`<system>`为“无”，绝不会产生 `UNKNOWN`。
                3. **提取答案时尽量忠实于用户原话**，但可适当归纳，确保 `answerInfo` 的 value 准确反映用户回答。
                4.**拒绝原因**仅当用户明确提供时才提取；若用户拒绝但未给原因`refuseReason`为空字符串即可。
                
                
                """.replace("{askUserQuestion}", AIConstants.ToolName.ASK_USER_QUESTION);
    }


}
