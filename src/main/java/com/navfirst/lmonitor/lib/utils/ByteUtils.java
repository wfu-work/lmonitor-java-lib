package com.navfirst.lmonitor.lib.utils;

import java.nio.charset.StandardCharsets;

/**
 * 创建：馥溪凝
 * 日期：2022/04/24 14:51
 * 描述：com.navfirst.lmonitor.lib.utils
 */
public class ByteUtils {

    public static String byteToStr(byte[] buffer) {
        try {

            int length = 0;
            for (int i = 0; i < buffer.length; ++i) {
                if (buffer[i] == 0) {
                    length = i;
                    break;
                }
            }
            return new String(buffer, 0, length, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

}
