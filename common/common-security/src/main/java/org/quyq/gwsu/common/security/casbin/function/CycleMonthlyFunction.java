package org.quyq.gwsu.common.security.casbin.function;


import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import org.casbin.jcasbin.util.function.CustomFunction;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/5/1
 * @description 表达式：判断当前时间是否在每月指定日期的指定时间段内
 * 参数：arg1 - 日期，逗号分隔（1-31）
 *      arg2 - 开始时间，格式 HH:mm
 *      arg3 - 结束时间，格式 HH:mm
 */
public class CycleMonthlyFunction extends CustomFunction {

    public static final String NAME = "cycleMonthly";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        String monthDaysStr = (String) arg1.getValue(env);
        String startTimeStr = (String) arg2.getValue(env);
        String endTimeStr = (String) arg3.getValue(env);

        try {
            LocalDateTime now = LocalDateTime.now();

            Set<Integer> monthDays = Arrays.stream(monthDaysStr.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());

            int currentDayOfMonth = now.getDayOfMonth();
            if (!monthDays.contains(currentDayOfMonth)) {
                return AviatorBoolean.FALSE;
            }

            return AviatorBoolean.valueOf(CycleWeeklyFunction.isInTimeRange(now, startTimeStr, endTimeStr));
        } catch (Exception e) {
            return AviatorBoolean.FALSE;
        }
    }
}
