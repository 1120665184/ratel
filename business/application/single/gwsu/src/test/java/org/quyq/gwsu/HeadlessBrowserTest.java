package org.quyq.gwsu;


import io.agentscope.core.agui.event.AguiEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.security.headless.HeadlessAgentListener;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

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
    public void sendMessage(){

        browserManager.sendMessage("1", "你好", new HeadlessAgentListener() {
            @Override
            public void onEvent(AguiEvent event) {
                objectMapper.writeValueAsString(event);
            }
        });

    }


}
