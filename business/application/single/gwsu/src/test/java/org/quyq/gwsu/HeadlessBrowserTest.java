package org.quyq.gwsu;


import io.agentscope.core.agui.event.AguiEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.security.headless.HeadlessAgentListener;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.quyq.gwsu.security.headless.HeadlessPageWrapper;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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

                    File file1 = file.get();
                    if(Objects.nonNull(file1)) {
                        try {
                            Files.delete(file1.toPath());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
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
        }catch (Exception e){
            log.info("" , e);
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

}
