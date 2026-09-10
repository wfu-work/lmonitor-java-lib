package com.navfirst.lmonitor.lib.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 创建：馥溪凝
 * 日期：2022/04/11 21:24
 * 描述：com.navfirst.lmonitor.lib.enums
 */
@Getter
@AllArgsConstructor
public enum ObsErrorEnum {

    NORMAL(0,"正常"),
    ROVER_RTCM_NOT_UPDATE(1, "监测站数据未更新"),
    BASE_RTCM_NOT_UPDATE(2, "基准站数据未更新"),
    ALL_RTCM_NOT_UPDATE(3, "基准站和监测站数据都未更新"),
    ROVER_RTCM_NOT_ENOUGH(4, "监测站数据不足"),
    BASE_RTCM_NOT_ENOUGH(8, "基准站数据不足"),
    ALL_RTCM_NOT_ENOUGH(12, "基准站和监测站数据都不足"),
    ROVER_RTCM_ERROR_FORMAT(16, "监测站无数据或数据格式不为rtcm3"),
    ROVER_NO_DATA(21,"监测站无数据或数据格式不为rtcm3"),
    RTCM_ERROR_FORMAT(29, "监测站无数据或基准站数据不足"),
    BASE_RTCM_ERROR_FORMAT(32, "基准站无数据或数据格式不为rtcm3"),
    BASE_NO_DATA(42, "基准站无数据"),
    NO_ALL_DATA(63, "任何数据都没有"),
    OTHER(999,"基准站或监测站数据不足");

    private final int code;
    private final String msg;

    public static String getValue(int code) {
        ObsErrorEnum obsErrorEnum = Arrays.stream(values()).filter(value -> value.code == code).findFirst().orElse(OTHER);
        if (null != obsErrorEnum) return obsErrorEnum.getMsg();
        return null;
    }

}
