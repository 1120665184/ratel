package org.quyq.gwsu.kit.api.job;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobInfoBuilderTest {

    @Test
    void beanModel_should_build_create_dto() {
        JobInfoCreateDTO dto = JobInfoBuilder.beanModel("demoHandler")
                .jobName("演示任务", "tester")
                .executorParam("{\"k\":\"v\"}")
                .alarmEmail("ops@test.com")
                .routeStrategy("ROUND")
                .misfireStrategy("DO_NOTHING")
                .blockStrategy("SERIAL_EXECUTION")
                .executorTimeout(120)
                .executorFailRetryCount(2)
                .scheduleCron("0 0/5 * * * ?")
                .childJobIds(List.of("101", "102"))
                .build();

        assertEquals("BEAN", dto.getJobMode());
        assertEquals("demoHandler", dto.getExecutorHandler());
        assertEquals("{\"k\":\"v\"}", dto.getExecutorParam());
        assertEquals("演示任务", dto.getName());
        assertEquals("tester", dto.getAuthor());
        assertEquals("ops@test.com", dto.getAlarmEmail());
        assertEquals("ROUND", dto.getExecutorRouteStrategy());
        assertEquals("DO_NOTHING", dto.getMisfireStrategy());
        assertEquals("SERIAL_EXECUTION", dto.getExecutorBlockStrategy());
        assertEquals(120, dto.getExecutorTimeout());
        assertEquals(2, dto.getExecutorFailRetryCount());
        assertEquals("CRON", dto.getScheduleType());
        assertEquals("0 0/5 * * * ?", dto.getScheduleConf());
        assertEquals("101,102", dto.getChildJobId());
    }

    @Test
    void urlModel_should_build_create_dto() {
        JobInfoCreateDTO dto = JobInfoBuilder.urlModel("system", "/job/callback")
                .jobName("URL任务", "tester")
                .bodyJson("{\"bizId\":\"1\"}")
                .scheduleFixRate(60)
                .build();

        assertEquals("URL", dto.getJobMode());
        assertEquals("system", dto.getPrefix());
        assertEquals("/job/callback", dto.getUrl());
        assertEquals("{\"bizId\":\"1\"}", dto.getBodyJson());
        assertEquals("FIX_RATE", dto.getScheduleType());
        assertEquals("60", dto.getScheduleConf());
    }

    @Test
    void glueModel_should_fill_default_glue_remark() {
        JobInfoCreateDTO dto = JobInfoBuilder.glueModel("GLUE_GROOVY", "println 'ok'")
                .jobName("GLUE任务", "tester")
                .scheduleNone()
                .build();

        assertEquals("GLUE", dto.getJobMode());
        assertEquals("GLUE_GROOVY", dto.getGlueType());
        assertEquals("println 'ok'", dto.getGlueSource());
        assertEquals("GLUE代码初始化", dto.getGlueRemark());
        assertEquals("NONE", dto.getScheduleType());
    }

    @Test
    void build_should_reject_missing_required_fields() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                JobInfoBuilder.beanModel("demoHandler").build());

        assertEquals("任务名称和负责人不能为空", exception.getMessage());
    }
}
