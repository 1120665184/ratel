package org.quyq.gwsu.common.security.casbin.function;


import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import org.casbin.jcasbin.util.function.CustomFunction;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/5/1
 * @description 表达式：判断当前时间是否在每周指定日期的指定时间段内
 * 参数：arg1 - 当前时间（r.env.datatime），支持毫秒值/LocalDateTime/Date
 *      arg2 - 星期几，逗号分隔（1-7，1=周一，7=周日）
 *      arg3 - 开始时间，格式 HH:mm（为空表示00:00:00）
 *      arg4 - 结束时间，格式 HH:mm（为空表示23:59:59）
 */
public class CycleWeeklyFunction extends CustomFunction {

    public static final String NAME = "cycleWeekly";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3, AviatorObject arg4) {
        Object datatimeValue = arg1.getValue(env);
        String weekDaysStr = (String) arg2.getValue(env);
        String startTimeStr = (String) arg3.getValue(env);
        String endTimeStr = (String) arg4.getValue(env);

        try {
            LocalDateTime now = TimeInRangeFunction.parseDateTime(datatimeValue);

            Set<Integer> weekDays = Arrays.stream(weekDaysStr.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());

            int currentDayOfWeek = now.getDayOfWeek().getValue();
            if (!weekDays.contains(currentDayOfWeek)) {
                return AviatorBoolean.FALSE;
            }

            return AviatorBoolean.valueOf(isInTimeRange(now, startTimeStr, endTimeStr));
        } catch (Exception e) {
            return AviatorBoolean.FALSE;
        }
    }

    static boolean isInTimeRange(LocalDateTime now, String startTimeStr, String endTimeStr) {
        LocalTime currentTime = now.toLocalTime();
        LocalTime startTime = (startTimeStr != null && !startTimeStr.isEmpty())
                ? LocalTime.parse(startTimeStr, TIME_FORMATTER) : LocalTime.MIN;
        LocalTime endTime = (endTimeStr != null && !endTimeStr.isEmpty())
                ? LocalTime.parse(endTimeStr, TIME_FORMATTER) : LocalTime.MAX;
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }
}
