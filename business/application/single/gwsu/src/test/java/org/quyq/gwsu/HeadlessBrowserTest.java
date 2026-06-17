package org.quyq.gwsu;


import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.google.gson.Gson;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.message.Msg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.security.constants.SerConstants;
import org.quyq.gwsu.security.headless.HeadlessAgentListener;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.quyq.gwsu.security.headless.domain.HeadlessCallConfig;
import org.quyq.gwsu.security.headless.service.IHeadlessService;
import org.quyq.gwsu.security.headless.service.impl.HeadlessServiceImpl;
import org.quyq.gwsu.security.headless.session.HeadlessPageWrapper;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Quyq
 * @date 2026/6/13
 * @description
 */
@SpringBootTest
@Slf4j
public class HeadlessBrowserTest {

    @Resource
    private HeadlessBrowserManager browserManager;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private IHeadlessService headlessService;


    @Test
    public void sendMessage() throws Exception{
        AtomicReference<File> file = new AtomicReference<>();
        try {
            browserManager.sendMessage("1", "帮我跳转到用户管理界面", new HeadlessAgentListener() {
                @Override
                public void onRunStarted(AguiEvent.RunStarted event, HeadlessPageWrapper wrapper) {
                    wrapper.startRecording();
                    log.info("智能体开始");
                }


                @Override
                public void onRunFinished(AguiEvent.RunFinished event, HeadlessPageWrapper wrapper) {
                    file.set(wrapper.stopRecording());
                    log.info("智能体结束");

                }

                @Override
                public void onTextMessageStart(AguiEvent.TextMessageStart event, HeadlessPageWrapper wrapper) {
                    log.info("文本消息开始");
                }

                @Override
                public void onTextMessageContent(String delta, HeadlessPageWrapper wrapper) {
                    log.info("输出内容：{}" , delta);
                }

                @Override
                public void onHumanApproval(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {
                    log.info("审批事件:{}" , objectMapper.writeValueAsString(event));
                }

                @Override
                public void onAskUserQuestion(String toolCallId, Map<String, Object> questions, HeadlessPageWrapper wrapper) {
                    log.info("询问用户问题：{}" , objectMapper.writeValueAsString(questions));
                }

                @Override
                public void onAgentOutput(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {
                    log.info("输出面板内容：{}" , objectMapper.writeValueAsString(event));
                }

                @Override
                public void onError(Throwable error, HeadlessPageWrapper wrapper) {
                    log.info("执行错误:{}" ,error.getMessage());
                }
            });

        }
        finally {
            if(Objects.nonNull(file.get())){
                Files.delete(file.get().toPath());
            }
        }


    }

    @Test
    public void userAnswer(){
        browserManager.userAnswer("1" ,"call_f103a5a252e245f9861f4ca1" ,Map.of("请问您今天需要我帮您做什么？" ,"咨询问题"),
                new HeadlessAgentListener() {
                    @Override
                    public void onEvent(AguiEvent event, HeadlessPageWrapper wrapper) {
                        log.info("事件：{}" , objectMapper.writeValueAsString(event));
                    }
                });
    }

    @Test
    public void approval(){
        browserManager.approval("1" , false , "不想跳转了" ,new HeadlessAgentListener() {
            @Override
            public void onEvent(AguiEvent event, HeadlessPageWrapper wrapper) {
                log.info("事件：{}" , objectMapper.writeValueAsString(event));
            }
        });
    }

    @Test
    public void newSession(){
        browserManager.newSession("1");
    }


    @Test
    public void graph(){
        Gson gson = new Gson();
        Message message = headlessService.stream("数据查询", HeadlessCallConfig.builder()
                        .userId("1")
                        .build())
                .doOnNext(v -> System.out.println("内容输出：" + gson.toJson(v)))
                .doOnComplete(() -> {
                    System.out.println("完成");
                })
                .blockLast();



        System.out.println(1);
//        CompiledGraph headlessGraph = headlessService.headlessGraph;
//
//        headlessGraph.stream(Map.of(
//                SerConstants.Headless.GRAPH_PARAM_QUERY , "你好",
//                SerConstants.Headless.GRAPH_PARAM_USER_ID , "1",
//                SerConstants.Headless.GRAPH_PARAM_THREAD_ID , ""
//        ))
//                .doOnNext(output -> {
//                    // 处理流式输出
//                    if (output instanceof StreamingOutput<?> streamingOutput) {
//                        // 流式输出块
//                        String chunk = streamingOutput.chunk();
//                        if (chunk != null && !chunk.isEmpty()) {
//                            System.out.print(chunk); // 实时打印流式内容
//                        }
//                    }
//                    else {
//                        // 普通节点输出
//                        String nodeId = output.node();
//                        Map<String, Object> state = output.state().data();
//
//                        if (state.containsKey("result")) {
//                            System.out.println("最终结果: " + state.get("result"));
//                        }
//                    }
//                })
//                .doOnComplete(() -> {
//
//                })
//                .doOnError(error -> {
//                    System.err.println("流式输出错误: " + error.getMessage());
//                })
//                .blockLast();
    }

}
