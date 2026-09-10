package com.navfirst.lmonitor.lib.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建：馥溪凝
 * 日期：2022/04/09 15:23
 * 描述：com.navfirst.dmonitor.lib.domains
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 4168483660656762466L;

    /**
     * 解算状态，0代表SPP，2代表动态，3代表静态
     */
    @Builder.Default
    private int rtMode = 3;

    /**
     * 开始时间 格式2026/05/20 00:00:00
     */
    private String timeStart;

    /**
     * 结束时间 格式2026/05/20 01:00:00
     */
    private String timeEnd;

    /**
     * 滤波时长(单位s)
     */
    private Integer filterPeriod;

    /**
     * 任务间隔(单位s)
     */
    private Integer processInterval;

    /**
     * 采样频率默认不写 算法会自动估算频率
     */
    private Integer sample;

    /**
     * 是否vrs
     */
    @Builder.Default
    private int vrs = 0;

    /**
     * 监测站
     */
    private String roverName;

    /**
     * 基准站
     */
    private String baseName;

    /**
     * 星历文件流
     */
    private byte[] brdcBytes;

    /**
     * 基站文件流
     */
    private byte[] baseBytes;

    /**
     * 测站文件流
     */
    private byte[] roverBytes;

    /**
     * 输出结果：0-历元解 1-单一解
     */
    @Builder.Default
    private int outMode = 1;

    /**
     * 输出解算结果文件
     */
    private String outFile;

    /**
     * 固定率
     */
    private Double minFixedRate;

    /**
     * 任务 0-准实时 1-重算
     */
    private int taskType;

    /**
     * 卫星系统
     */
    private String navSys;

    /**
     * 北斗频段 0：B1I+B3I 1：B1C+B2A
     */
    private int bds;

    /**
     * 额外信息
     */
    private String extra;

    /**
     * 基站x
     */
    private Double baseX;

    /**
     * 基站y
     */
    private Double baseY;

    /**
     * 基站z
     */
    private Double baseZ;

}
