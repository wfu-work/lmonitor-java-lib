package com.navfirst.lmonitor.lib.services.impl;

import com.navfirst.lmonitor.lib.domains.MonitorData;
import com.navfirst.lmonitor.lib.domains.MonitorTask;
import com.navfirst.lmonitor.lib.enums.NavErrorEnum;
import com.navfirst.lmonitor.lib.enums.ObsErrorEnum;
import com.navfirst.lmonitor.lib.services.HandlerDataInterface;
import com.navfirst.lmonitor.lib.services.MonitorDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 创建：馥溪凝
 * 日期：2022/04/10 13:40
 * 描述：com.navfirst.lmonitor.lib.services.impl
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MonitorDataServiceImpl implements MonitorDataService {

    private HandlerDataInterface handlerDataInterface;

    @Override
    public void handlerData(MonitorTask monitorTask, String data, String errMsg) {
        if (StringUtils.isBlank(data)) return;
        log.error("解算结果：{}", data);
        String[] splits = Arrays.stream(data.split(" ")).filter(StringUtils::isNotBlank).toArray(String[]::new);
        if (splits.length >= 23) {
            MonitorData monitorData = getMonitorDataBySplits(monitorTask, splits);
            if (StringUtils.isNotBlank(errMsg)) {
                monitorData.setErrMsg(errMsg);
            }
            if (null != this.handlerDataInterface) {
                this.handlerDataInterface.handlerData(monitorData);
            }
        }
    }

    @Override
    public MonitorData handlerDataSync(MonitorTask monitorTask, String data, String errMsg) {
        if (StringUtils.isBlank(data)) return null;
        String[] splits = Arrays.stream(data.split(" ")).filter(StringUtils::isNotBlank).toArray(String[]::new);
        if (splits.length >= 23) {
            MonitorData monitorData = getMonitorDataBySplits(monitorTask, splits);
            if (StringUtils.isNotBlank(errMsg)) {
                monitorData.setErrMsg(errMsg);
            }
            return monitorData;
        }
        return null;
    }

    private MonitorData getMonitorDataBySplits(MonitorTask monitorTask, String[] splits) {
        String gpsTime, lastObsTime, solStatus, offTime;
        double E, N, U;
        double fixedRate = 0.0, dposmax = 0.0, dposavg = 0.0, dposstd = 0.0, baseEpochRate = 0.0, roverEpochRate = 0.0;
        int roverSample, baseSample, satNum, roverObsNum, baseObsNum, fileStatus, navStatus, solutionType;
        MonitorData monitorData;
        if (splits.length == 23) {
            gpsTime = splits[2] + " " + splits[3];
            lastObsTime = gpsTime;
            dposmax = Double.parseDouble(splits[4]);
            dposavg = Double.parseDouble(splits[5]);
            dposstd = Double.parseDouble(splits[6]);
            fixedRate = Double.parseDouble(splits[7]);
            roverEpochRate = Double.parseDouble(splits[8]);
            baseEpochRate = Double.parseDouble(splits[9]);
            E = Double.parseDouble(splits[10]);
            N = Double.parseDouble(splits[11]);
            U = Double.parseDouble(splits[12]);
            solStatus = splits[13];
            solutionType = Integer.parseInt(splits[14]);
            satNum = Integer.parseInt(splits[15]);
            roverSample = Integer.parseInt(splits[16]);
            baseSample = Integer.parseInt(splits[17]);
            roverObsNum = Integer.parseInt(splits[18]);
            baseObsNum = Integer.parseInt(splits[19]);
            fileStatus = Integer.parseInt(splits[20]);
            navStatus = Integer.parseInt(splits[21]);
            offTime = splits[22];
        } else if (splits.length == 24) {
            gpsTime = splits[2] + " " + splits[3];
            lastObsTime = gpsTime;
            dposmax = Double.parseDouble(splits[4]);
            dposavg = Double.parseDouble(splits[5]);
            dposstd = Double.parseDouble(splits[6]);
            fixedRate = Double.parseDouble(splits[7]);
            roverEpochRate = Double.parseDouble(splits[8]);
            baseEpochRate = Double.parseDouble(splits[9]);
            E = Double.parseDouble(splits[10]);
            N = Double.parseDouble(splits[11]);
            U = Double.parseDouble(splits[12]);
            solStatus = splits[13];
            solutionType = Integer.parseInt(splits[14]);
            satNum = Integer.parseInt(splits[15]);
            roverSample = Integer.parseInt(splits[17]);
            baseSample = Integer.parseInt(splits[18]);
            roverObsNum = Integer.parseInt(splits[19]);
            baseObsNum = Integer.parseInt(splits[20]);
            fileStatus = Integer.parseInt(splits[21]);
            navStatus = Integer.parseInt(splits[22]);
            offTime = splits[23];
        } else {
            gpsTime = splits[2] + " " + splits[3];
            lastObsTime = gpsTime;
            E = Double.parseDouble(splits[4]);
            N = Double.parseDouble(splits[5]);
            U = Double.parseDouble(splits[6]);
            solStatus = splits[7];
            if (StringUtils.equals(splits[8], "0.0000")) {
                satNum = 0;
            } else {
                satNum = Integer.parseInt(splits[8]);
            }
            try {
                roverObsNum = Integer.parseInt(splits[9]);
            } catch (Exception e) {
                roverObsNum = 0;
            }
            baseObsNum = Integer.parseInt(splits[10]);
            fileStatus = Integer.parseInt(splits[11]);
            navStatus = Integer.parseInt(splits[12]);
            offTime = splits[13];
            roverSample = 0;
            baseSample = 0;
            solutionType = 0;
        }
        monitorData = MonitorData.builder().baseName(monitorTask.getBaseName().trim()).roverName(monitorTask.getRoverName().trim()).gpsTime(gpsTime).lastObsTime(lastObsTime).E(E).N(N).U(U).fixedRate(fixedRate).dposmax(dposmax).dposavg(dposavg).dposstd(dposstd).solStatus(solStatus).satNum(satNum).roverObsNum(roverObsNum).baseObsNum(baseObsNum).fileStatus(fileStatus).fileStatusDesc(ObsErrorEnum.getValue(fileStatus)).navStatus(navStatus).navStatusDesc(NavErrorEnum.getValue(navStatus)).offTime(offTime).rtMode(monitorTask.getRtMode()).filterPeriod(monitorTask.getFilterPeriod()).processInterval(monitorTask.getProcessInterval()).vrs(monitorTask.getVrs()).taskType(monitorTask.getTaskType()).outMode(monitorTask.getOutMode()).roverSample(roverSample).baseSample(baseSample).baseEpochRate(baseEpochRate).roverEpochRate(roverEpochRate).solutionType(solutionType).navSys(StringUtils.isNotBlank(monitorTask.getNavSys()) ? monitorTask.getNavSys() : "1,4,8,32").extra(monitorTask.getExtra()).build();
        return monitorData;
    }

    @Override
    public void setHandlerData(HandlerDataInterface handlerDataInterface) {
        this.handlerDataInterface = handlerDataInterface;
    }

}
