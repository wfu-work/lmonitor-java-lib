package com.navfirst.lmonitor.lib.utils;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 创建：馥溪凝
 * 日期：2026/7/6 17:24
 * 描述：com.navfirst.lmonitor.lib.utils
 */
public class TimeUtils {

     public static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    };

    public static double[] parseEpoch(String value) {
        if (StringUtils.isBlank(value)) {
            return new double[6];
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
                return new double[]{
                        dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
                        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond()
                };
            } catch (DateTimeParseException ignored) {
                // Try the next supported monitor time format.
            }
        }
        throw new IllegalArgumentException("监测时间格式错误：" + value);
    }
}
