package org.quyq.gwsu.kit.api.job;

import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 服务间创建定时任务的构建器。
 */
public final class JobInfoBuilder {

    private static final String MODE_BEAN = "BEAN";
    private static final String MODE_URL = "URL";
    private static final String MODE_GLUE = "GLUE";
    private static final String SCHEDULE_NONE = "NONE";
    private static final String SCHEDULE_CRON = "CRON";
    private static final String SCHEDULE_FIX_RATE = "FIX_RATE";
    private static final String DEFAULT_GLUE_REMARK = "GLUE代码初始化";

    private JobInfoBuilder() {
    }

    public static BeanBuilder beanModel(String executorHandler) {
        return new BeanBuilder(executorHandler);
    }

    public static UrlBuilder urlModel(String prefix, String url) {
        return new UrlBuilder(prefix, url);
    }

    public static GlueBuilder glueModel(String glueType, String glueSource) {
        return new GlueBuilder(glueType, glueSource);
    }

    public static class BeanBuilder extends AbstractBuilder<BeanBuilder> {

        private BeanBuilder(String executorHandler) {
            dto.setJobMode(MODE_BEAN);
            dto.setExecutorHandler(executorHandler);
        }

        public BeanBuilder executorParam(String executorParam) {
            dto.setExecutorParam(executorParam);
            return this;
        }
    }

    public static class UrlBuilder extends AbstractBuilder<UrlBuilder> {

        private UrlBuilder(String prefix, String url) {
            dto.setJobMode(MODE_URL);
            dto.setPrefix(prefix);
            dto.setUrl(url);
        }

        public UrlBuilder bodyJson(String bodyJson) {
            dto.setBodyJson(bodyJson);
            return this;
        }
    }

    public static class GlueBuilder extends AbstractBuilder<GlueBuilder> {

        private GlueBuilder(String glueType, String glueSource) {
            dto.setJobMode(MODE_GLUE);
            dto.setGlueType(glueType);
            dto.setGlueSource(glueSource);
        }

        public GlueBuilder glueRemark(String glueRemark) {
            dto.setGlueRemark(glueRemark);
            return this;
        }
    }

    public abstract static class AbstractBuilder<T extends AbstractBuilder<T>> {

        protected final JobInfoCreateDTO dto = new JobInfoCreateDTO();

        public T jobName(String name, String author) {
            dto.setName(name);
            dto.setAuthor(author);
            return self();
        }

        public T alarmEmail(String alarmEmail) {
            dto.setAlarmEmail(alarmEmail);
            return self();
        }

        public T routeStrategy(String strategy) {
            dto.setExecutorRouteStrategy(strategy);
            return self();
        }

        public T misfireStrategy(String strategy) {
            dto.setMisfireStrategy(strategy);
            return self();
        }

        public T blockStrategy(String strategy) {
            dto.setExecutorBlockStrategy(strategy);
            return self();
        }

        public T executorTimeout(int seconds) {
            dto.setExecutorTimeout(seconds);
            return self();
        }

        public T executorFailRetryCount(int count) {
            dto.setExecutorFailRetryCount(count);
            return self();
        }

        public T scheduleCron(String cron) {
            dto.setScheduleType(SCHEDULE_CRON);
            dto.setScheduleConf(cron);
            return self();
        }

        public T scheduleFixRate(int seconds) {
            dto.setScheduleType(SCHEDULE_FIX_RATE);
            dto.setScheduleConf(String.valueOf(seconds));
            return self();
        }

        public T scheduleNone() {
            dto.setScheduleType(SCHEDULE_NONE);
            dto.setScheduleConf(null);
            return self();
        }

        public T childJobIds(List<String> ids) {
            dto.setChildJobId(ids == null || ids.isEmpty() ? null : String.join(",", ids));
            return self();
        }

        public JobInfoCreateDTO build() {
            validateCommon();
            validateByMode();
            if (MODE_GLUE.equals(dto.getJobMode()) && !StringUtils.hasText(dto.getGlueRemark())) {
                dto.setGlueRemark(DEFAULT_GLUE_REMARK);
            }
            if (!StringUtils.hasText(dto.getScheduleType())) {
                dto.setScheduleType(SCHEDULE_NONE);
            }
            return dto;
        }

        private void validateCommon() {
            if (!StringUtils.hasText(dto.getName()) || !StringUtils.hasText(dto.getAuthor())) {
                throw new IllegalArgumentException("任务名称和负责人不能为空");
            }
        }

        private void validateByMode() {
            if (MODE_BEAN.equals(dto.getJobMode()) && !StringUtils.hasText(dto.getExecutorHandler())) {
                throw new IllegalArgumentException("BEAN模式的执行器不能为空");
            }
            if (MODE_URL.equals(dto.getJobMode())
                    && (!StringUtils.hasText(dto.getPrefix()) || !StringUtils.hasText(dto.getUrl()))) {
                throw new IllegalArgumentException("URL模式的前缀和地址不能为空");
            }
            if (MODE_GLUE.equals(dto.getJobMode())
                    && (!StringUtils.hasText(dto.getGlueType()) || !StringUtils.hasText(dto.getGlueSource()))) {
                throw new IllegalArgumentException("GLUE模式的类型和源码不能为空");
            }
        }

        @SuppressWarnings("unchecked")
        private T self() {
            return (T) this;
        }
    }
}
