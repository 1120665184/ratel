package org.quyq.gwsu.headless;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Quyq
 * @date 2026/6/25
 * @description 无头智能体服务启动类
 */
@SpringBootApplication
@Slf4j
public class HeadlessApplication {

    static void main() {
        SpringApplication.run(HeadlessApplication.class);
    }

}
