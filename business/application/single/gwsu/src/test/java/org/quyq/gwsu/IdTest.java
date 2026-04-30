package org.quyq.gwsu;


import cn.hutool.core.util.IdUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.cache.utils.IDGenerationUtils;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Quyq
 * @date 2026/3/20
 * @description
 */
@SpringBootTest
public class IdTest {

    @Resource
    private IDGenerationUtils generationUtils;


    @Test
    public void generateNextIdTest() {
        long l = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
           System.out.println(generationUtils.generateNextIdStr());
        }

        System.out.println("耗时：" + (System.currentTimeMillis() - l)/1000);

    }

}
