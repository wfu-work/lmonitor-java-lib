package com.navfirst.lmonitor.lib.library;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * 创建：馥溪凝
 * 日期：2022/04/09 15:23
 * 描述：com.navfirst.dmonitor.lib.library
 */
public interface MonitorLibrary extends Library {

    String os = System.getProperty("os.name");
    boolean isWindowOs = os != null && os.startsWith("Windows");
    boolean isMacOs = os != null && os.startsWith("Mac");

    String RTKCONV_NAME = "LMonitor";

    MonitorLibrary INSTANCE = Native.load(RTKCONV_NAME, MonitorLibrary.class);

    MonitorLibrary INSTANTCES = (MonitorLibrary) Native.synchronizedLibrary(
            Native.load(RTKCONV_NAME, MonitorLibrary.class)
    );

    String startLMonitor(MonitorStreamInfo monitorInfo, String license);

    String getLVersion();

    String getUuid();

}
