package com.navfirst.lmonitor.lib.library;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import lombok.*;

import java.util.List;

/**
 * 创建：馥溪凝
 * 日期：2022/04/09 15:19
 * 描述：LMonitorStreamInfo 的 JNA 映射，字段类型和顺序必须与原生结构体一致。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Structure.FieldOrder({
        "calMode", "es", "ee", "ti", "vrsMode", "navsys", "solstatic", "fixThresh", "rb", "outfile",
        "brdcBuf", "brdcLen", "roverBuf", "roverLen", "baseBuf", "baseLen", "precBuf", "precLen",
        "ionoopt", "tropopt", "armode", "sateph", "nf", "minfix", "warmupMin"
})
public class MonitorStreamInfo extends MyStructure {

    private static final List<String> FIELD_ORDER = List.of(
            "calMode", "es", "ee", "ti", "vrsMode", "navsys", "solstatic", "fixThresh", "rb", "outfile",
            "brdcBuf", "brdcLen", "roverBuf", "roverLen", "baseBuf", "baseLen", "precBuf", "precLen",
            "ionoopt", "tropopt", "armode", "sateph", "nf", "minfix", "warmupMin"
    );

    @Builder.Default
    public int calMode = 3;

    @Builder.Default
    public double[] es = new double[6];

    @Builder.Default
    public double[] ee = new double[6];

    public int ti;

    @Builder.Default
    public int vrsMode = 0;

    public int navsys;

    @Builder.Default
    public int solstatic = 1;

    public double fixThresh;

    @Builder.Default
    public double[] rb = new double[3];

    @Builder.Default
    public String outfile = "";

    public Pointer brdcBuf;

    public long brdcLen;

    public Pointer roverBuf;

    public long roverLen;

    public Pointer baseBuf;

    public long baseLen;

    public Pointer precBuf;

    public long precLen;

    /** 电离层选项，0 使用原生默认值 IFLC。 */
    public int ionoopt;

    /** 对流层选项，0 使用原生默认值 ZTD 估计。 */
    public int tropopt;

    /** 模糊度固定模式，0 使用原生默认值 continuous。 */
    public int armode;

    /** 星历选项，0 自动选择：有精密星历用 PREC，否则用 BRDC。 */
    public int sateph;

    /** 频点数，0 使用原生默认值 2。 */
    public int nf;

    /** 首次固定需连续通过的历元数，0 使用原生默认值 5。 */
    public int minfix;

    /** 对应 warmup_min，预热时长（分钟），0 关闭。 */
    public double warmupMin;

    private transient Memory brdcMemory;

    private transient Memory roverMemory;

    private transient Memory baseMemory;

    private transient Memory precMemory;

    @Override
    protected List<String> getFieldOrder() {
        return FIELD_ORDER;
    }

    public void setBrdc(byte[] brdc) {
        this.brdcMemory = toMemory(brdc);
        this.brdcBuf = brdcMemory;
        this.brdcLen = byteLength(brdc);
    }

    public void setRover(byte[] rover) {
        this.roverMemory = toMemory(rover);
        this.roverBuf = roverMemory;
        this.roverLen = byteLength(rover);
    }

    public void setBase(byte[] base) {
        this.baseMemory = toMemory(base);
        this.baseBuf = baseMemory;
        this.baseLen = byteLength(base);
    }

    public void setPrec(byte[] prec) {
        this.precMemory = toMemory(prec);
        this.precBuf = precMemory;
        this.precLen = byteLength(prec);
    }

    private static Memory toMemory(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        Memory memory = new Memory(bytes.length);
        memory.write(0, bytes, 0, bytes.length);
        return memory;
    }

    private static long byteLength(byte[] bytes) {
        return bytes == null ? 0L : bytes.length;
    }

}
