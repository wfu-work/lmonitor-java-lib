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
        String[] splits = splitFields(data);
        if (isSupportedResultLength(splits.length)) {
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
        String[] splits = splitFields(data);
        if (isSupportedResultLength(splits.length)) {
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
        double E, N, U, X = 0.0, Y = 0.0, Z = 0.0, B = 0.0, L = 0.0, H = 0.0;
        double fixedRate = 0.0, dposmax = 0.0, dposavg = 0.0, dposstd = 0.0, baseEpochRate = 0.0, roverEpochRate = 0.0;
        int roverSample, baseSample, satNum, roverObsNum, baseObsNum, fileStatus, navStatus, solutionType, isMoved = 0;
        MonitorData monitorData;
        if (splits.length == 30) {
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
            X = Double.parseDouble(splits[13]);
            Y = Double.parseDouble(splits[14]);
            Z = Double.parseDouble(splits[15]);
            B = Double.parseDouble(splits[16]);
            L = Double.parseDouble(splits[17]);
            H = Double.parseDouble(splits[18]);
            solStatus = splits[19];
            solutionType = Integer.parseInt(splits[20]);
            satNum = Integer.parseInt(splits[21]);
            isMoved = Integer.parseInt(splits[22]);
            roverSample = Integer.parseInt(splits[23]);
            baseSample = Integer.parseInt(splits[24]);
            roverObsNum = Integer.parseInt(splits[25]);
            baseObsNum = Integer.parseInt(splits[26]);
            fileStatus = Integer.parseInt(splits[27]);
            navStatus = Integer.parseInt(splits[28]);
            offTime = splits[29];
        } else if (splits.length == 23) {
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
            isMoved = Integer.parseInt(splits[16]);
            roverSample = Integer.parseInt(splits[17]);
            baseSample = Integer.parseInt(splits[18]);
            roverObsNum = Integer.parseInt(splits[19]);
            baseObsNum = Integer.parseInt(splits[20]);
            fileStatus = Integer.parseInt(splits[21]);
            navStatus = Integer.parseInt(splits[22]);
            offTime = splits[23];
        } else {
            throw new IllegalArgumentException("不支持的解算结果字段数: " + splits.length);
        }
        monitorData = MonitorData.builder().baseName(monitorTask.getBaseName().trim()).roverName(monitorTask.getRoverName().trim()).gpsTime(gpsTime).lastObsTime(lastObsTime).E(E).N(N).U(U).X(X).Y(Y).Z(Z).B(B).L(L).H(H).fixedRate(fixedRate).dposmax(dposmax).dposavg(dposavg).dposstd(dposstd).solStatus(solStatus).satNum(satNum).isMoved(isMoved).roverObsNum(roverObsNum).baseObsNum(baseObsNum).fileStatus(fileStatus).fileStatusDesc(ObsErrorEnum.getValue(fileStatus)).navStatus(navStatus).navStatusDesc(NavErrorEnum.getValue(navStatus)).offTime(offTime).rtMode(monitorTask.getRtMode()).filterPeriod(monitorTask.getFilterPeriod()).processInterval(monitorTask.getProcessInterval()).vrs(monitorTask.getVrs()).taskType(monitorTask.getTaskType()).outMode(monitorTask.getOutMode()).roverSample(roverSample).baseSample(baseSample).baseEpochRate(baseEpochRate).roverEpochRate(roverEpochRate).solutionType(solutionType).navSys(StringUtils.isNotBlank(monitorTask.getNavSys()) ? monitorTask.getNavSys() : "1,4,8,32").extra(monitorTask.getExtra()).build();
        return monitorData;
    }

    private String[] splitFields(String data) {
        String value = StringUtils.trimToEmpty(data);
        return value.isEmpty() ? new String[0] : value.split("\\s+");
    }

    private boolean isSupportedResultLength(int length) {
        return length == 23 || length == 24 || length == 30;
    }

    @Override
    public void setHandlerData(HandlerDataInterface handlerDataInterface) {
        this.handlerDataInterface = handlerDataInterface;
    }

}
