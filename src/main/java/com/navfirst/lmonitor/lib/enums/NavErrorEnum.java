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
public enum NavErrorEnum {

    NORMAL(0,"正常"),
    ROVER_RTCM_NOT_NAV(1, "监测站无星历"),
    BASE_RTCM_NOT_NAV(2, "基准站无星历"),
    NAV_NOT_ENOUGH(4, "星历不足"),
    NAV_NOT_ROVER(5, "监测站无星历"),
    NAV_NOT_BASE(6, "基准站无星历"),
    NAV_FILE_NOT_ENOUGH(8, "设置的星历文件星历不足"),
    NAV_ERROR(15, "无任何星历");

    private final int code;
    private final String msg;

    public static String getValue(int code) {
        NavErrorEnum navErrorEnum = Arrays.stream(values()).filter(value -> value.code == code).findFirst().orElse(null);
        if (null != navErrorEnum) return navErrorEnum.getMsg();
        return null;
    }

}
