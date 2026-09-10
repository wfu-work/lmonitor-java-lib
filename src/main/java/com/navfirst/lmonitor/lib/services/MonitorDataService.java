package com.navfirst.lmonitor.lib.services;

import com.navfirst.lmonitor.lib.domains.MonitorData;
import com.navfirst.lmonitor.lib.domains.MonitorTask;

/**
 * 创建：馥溪凝
 * 日期：2022/04/10 13:40
 * 描述：com.navfirst.dmonitor.lib.services
 */
public interface MonitorDataService {

    MonitorData handlerDataSync(MonitorTask monitorTask, String data, String errMsg);

    void handlerData(MonitorTask monitorTask, String data, String errMsg);

    void setHandlerData(HandlerDataInterface handlerDataInterface);

}
