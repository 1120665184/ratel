package org.quyq.gwsu.security.connect.entrance;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.connect.entrance.dingtalk.DingTalkClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description 钉钉机器人 stream初始化
 */
@Component
@RequiredArgsConstructor
public class DingTalkEntrance implements ApplicationRunner {

    private final DingTalkClient dingTalkClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        init();
    }

    public void init() throws Exception {
        dingTalkClient.init();
    }

}
