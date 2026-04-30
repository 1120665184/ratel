package org.quyq.gwsu.security.abac.domain;


import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/15
 * @description 表达式实例上下文
 * 具体值根据不同的权限表达式生成类型而确定
 */
@Data
public class ExpressionContext {

    private String value;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalTime startTime;
    private LocalTime endTime;

    private Map<String, Object> extraParam = new HashMap<>();

    /**
     * 放入扩展参数
     *
     * @param key
     * @param value
     * @return
     */
    public ExpressionContext putExtraParam(String key, Object value) {
        if (Objects.isNull(extraParam)) {
            extraParam = new HashMap<>();
        }
        extraParam.put(key, value);
        return this;
    }


    @SuppressWarnings("unchecked")
    public <V> V getParam(String key) {
        return (V) extraParam.get(key);
    }

}
