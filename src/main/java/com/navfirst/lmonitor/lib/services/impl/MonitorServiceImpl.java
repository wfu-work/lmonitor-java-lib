package com.navfirst.lmonitor.lib.services.impl;

import com.navfirst.lmonitor.lib.domains.MonitorTask;
import com.navfirst.lmonitor.lib.exceptions.RtkconvException;
import com.navfirst.lmonitor.lib.library.MonitorLibrary;
import com.navfirst.lmonitor.lib.library.MonitorStreamInfo;
import com.navfirst.lmonitor.lib.services.HandlerDataInterface;
import com.navfirst.lmonitor.lib.services.MonitorDataService;
import com.navfirst.lmonitor.lib.services.MonitorService;
import com.navfirst.lmonitor.lib.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 创建：馥溪凝
 * 日期：2022/04/09 15:26
 * 描述：com.navfirst.lmonitor.lib.services.impl
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MonitorServiceImpl implements MonitorService {

    private final MonitorLibrary monitorLibrary = MonitorLibrary.INSTANCE;
    private final MonitorDataService monitorDataService;

    @Override
    public String getVersion() {
        return this.monitorLibrary.getLVersion();
    }

    @Override
    public String getUuid() {
        return this.monitorLibrary.getUuid();
    }

    /**
     * 初始化
     */
    @Override
    public void startMonitor(MonitorTask monitorTask, String license) {
        validateMonitorTask(monitorTask);
        MonitorStreamInfo monitorInfo = covertMonitorInfo(monitorTask);
        String result = this.monitorLibrary.startLMonitor(monitorInfo, license);
        String[] resultLines = splitMonitorResult(result);
        this.monitorDataService.handlerData(monitorTask, resultLines[0], resultLines[1]);
    }

    private void validateMonitorTask(MonitorTask monitorTask) {
        if (monitorTask == null) {
            throw new RtkconvException("监测任务不能为空");
        }
        requireBytes(monitorTask.getRoverBytes(), "监测站数据流不能为空");
        requireBytes(monitorTask.getBaseBytes(), "基准站数据流不能为空");
    }

    private void requireBytes(byte[] bytes, String message) {
        if (bytes == null || bytes.length == 0) {
            throw new RtkconvException(message);
        }
    }

    private String[] splitMonitorResult(String result) {
        String[] lines = StringUtils.defaultString(result).split("\\R", 2);
        String data = lines.length > 0 ? lines[0] : "";
        String errMsg = lines.length > 1 ? lines[1] : "";
        return new String[]{data, errMsg};
    }

    @Override
    public void setHandlerData(HandlerDataInterface handlerDataInterface) {
        this.monitorDataService.setHandlerData(handlerDataInterface);
    }

    private MonitorStreamInfo covertMonitorInfo(MonitorTask monitorTask) {
        int navSys;
        if (StringUtils.isBlank(monitorTask.getNavSys())) {
            navSys = 13;
        } else {
            navSys = Arrays.stream(monitorTask.getNavSys().split(",")).mapToInt(Integer::parseInt).sum();
        }
        if (monitorTask.getBds() == 0 && StringUtils.contains(monitorTask.getNavSys(), "32")){
            navSys -= 32;
        }
        MonitorStreamInfo monitorInfo = MonitorStreamInfo.builder()
                .calMode(monitorTask.getRtMode())
                .es(TimeUtils.parseEpoch(monitorTask.getTimeStart()))
                .ee(TimeUtils.parseEpoch(monitorTask.getTimeEnd()))
                .ti(defaultInt(monitorTask.getSample()))
                .vrsMode(monitorTask.getVrs())
                .navsys(navSys)
                .solstatic(monitorTask.getOutMode())
                .fixThresh(defaultDouble(monitorTask.getMinFixedRate()))
                .rb(new double[]{
                        defaultDouble(monitorTask.getBaseX()),
                        defaultDouble(monitorTask.getBaseY()),
                        defaultDouble(monitorTask.getBaseZ())
                })
                .outfile(StringUtils.defaultString(monitorTask.getOutFile()))
                .ionoopt(monitorTask.getIonoopt())
                .tropopt(monitorTask.getTropopt())
                .armode(monitorTask.getArmode())
                .sateph(monitorTask.getSateph())
                .nf(monitorTask.getNf())
                .minfix(monitorTask.getMinfix())
                .warmupMin(monitorTask.getWarmupMin())
                .build();
        monitorInfo.setBrdc(appendZeroTerminator(monitorTask.getBrdcBytes()));
        monitorInfo.setRover(monitorTask.getRoverBytes());
        monitorInfo.setBase(monitorTask.getBaseBytes());
        monitorInfo.setPrec(monitorTask.getPrecBytes());
        return monitorInfo;
    }

    private byte[] appendZeroTerminator(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        if (bytes[bytes.length - 1] == 0) {
            return bytes;
        }
        return Arrays.copyOf(bytes, bytes.length + 1);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double defaultDouble(Double value) {
        return value == null ? 0D : value;
    }

}
