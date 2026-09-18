# lmonitor-java-lib

`lmonitor-java-lib` 是 `libLMonitor` 原生库的 Java/JNA 封装，提供 GNSS 长基线监测解算能力。

当前工程内置的原生库：

- `src/main/resources/linux-x86-64/libLMonitor.so`
- `src/main/resources/linux-aarch64/libLMonitor.so`
- `src/main/resources/darwin-aarch64/libLMonitor.dylib`

核心调用入口有两层：

- 推荐：通过 Spring 注入 `MonitorService`
- 直接：通过 JNA 调用 `MonitorLibrary.INSTANCE.startLMonitor(...)`

## 运行要求

- JDK 20 或以上
- Spring Boot 3.2.x
- JNA 5.12.x
- 当前运行系统架构需要有对应的 `libLMonitor` 原生库
- 调用解算时必须提供 license 路径

如果在 IDE 或外部工程中出现 `UnsatisfiedLinkError`，优先检查：

- `libLMonitor.so` 或 `libLMonitor.dylib` 是否在 classpath 资源中
- 当前机器架构是否和资源目录匹配，例如 `linux-x86-64`、`linux-aarch64`、`darwin-aarch64`
- 必要时手动指定：

```bash
-Djna.library.path=/path/to/native/lib/dir
```

## 原生库方法

JNA 接口定义在：

```java
import com.navfirst.lmonitor.lib.library.MonitorLibrary;
```

当前暴露的方法：

```java
String getLVersion();

String getUuid();

String startLMonitor(MonitorStreamInfo monitorInfo, String license);
```

说明：

- `getLVersion()`：获取算法库版本。
- `getUuid()`：获取当前机器唯一标识，用于申请 license。
- `startLMonitor(...)`：调用原生库执行解算。

`startLMonitor` 返回值格式为：

```text
<solBuf>
<errMsg>
```

第一行是解算结果，第二行是错误信息。`MonitorService` 会自动拆分这两行并转换为 `MonitorData`。

## 推荐调用方式：MonitorService

在 Spring 环境中直接注入：

```java
import com.navfirst.lmonitor.lib.domains.MonitorTask;
import com.navfirst.lmonitor.lib.services.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class DemoService {

    private final MonitorService monitorService;

    public void run() throws Exception {
        Path brdcPath = Path.of("/path/to/BRDM1400.rnx");
        byte[] brdcBytes = Files.isRegularFile(brdcPath) ? Files.readAllBytes(brdcPath) : null;
        byte[] roverBytes = Files.readAllBytes(Path.of("/path/to/XPJZ01.2026140binRTCM3"));
        byte[] baseBytes = Files.readAllBytes(Path.of("/path/to/XPJZ02.2026140binRTCM3"));
        Path precPath = Path.of("/path/to/precise.sp3");
        byte[] precBytes = Files.isRegularFile(precPath) ? Files.readAllBytes(precPath) : null;

        MonitorTask task = MonitorTask.builder()
                .rtMode(3)
                .timeStart("2026/05/20 00:00:00")
                .timeEnd("2026/05/20 01:00:00")
                .sample(0)
                .vrs(0)
                .roverName("XPJZ01")
                .baseName("XPJZ02")
                .brdcBytes(brdcBytes)
                .roverBytes(roverBytes)
                .baseBytes(baseBytes)
                .precBytes(precBytes)
                .ionoopt(0)
                .tropopt(0)
                .armode(0)
                .sateph(0)
                .nf(0)
                .minfix(0)
                .warmupMin(0)
                .outMode(1)
                .minFixedRate(0.75)
                .navSys("1,4,8,32")
                .bds(1)
                .build();

        monitorService.setHandlerData(monitorData -> {
            System.out.println("解算结果: " + monitorData);
        });

        monitorService.startMonitor(task, "/path/to/license.lic");
    }
}
```

必填项：

- `roverBytes`：监测站 RTCM3 数据流。
- `baseBytes`：基准站 RTCM3 数据流。
- `license`：license 文件路径，作为 `startMonitor(task, license)` 第二个参数传入。

可选项：

- `brdcBytes`：广播星历文件流，通常是 BRDM/RINEX 文件内容；可以为空。
- `precBytes`：精密星历文件流（SP3 文本）；可以为空。`sateph=0` 时，有精密星历使用 PREC，否则使用 BRDC。

重要字段：

| 字段 | 说明 |
| --- | --- |
| `rtMode` | 解算模式：`0` SPP，`1` DGPS，`2` 动态，`3` 静态 |
| `timeStart` | 开始时间，支持 `yyyy/MM/dd HH:mm:ss` 和 `yyyy-MM-dd HH:mm:ss` |
| `timeEnd` | 结束时间，支持同上格式 |
| `sample` | 采样间隔；通常传 `0` |
| `vrs` | 是否 VRS：`0` 否，`1` 是 |
| `outMode` | 输出模式：`0` 历元解，`1` 单一解 |
| `minFixedRate` | 最小固定率，例如 `0.75` |
| `navSys` | 卫星系统聚合项，例如 `1,4,8,32` |
| `bds` | 北斗频段开关；为 `0` 时会从 `navSys` 中移除 `32` |
| `baseX/baseY/baseZ` | 基准站 ECEF 坐标，单位米；需要输出监测站绝对坐标时传入三个分量，可不传，默认 `0` |
| `outFile` | 底层算法输出文件路径，可不传 |
| `ionoopt` | 电离层选项，`0` 使用原生默认值 IFLC（无电离层组合） |
| `tropopt` | 对流层选项，`0` 使用原生默认值 ZTD（天顶对流层延迟）估计 |
| `armode` | 模糊度固定模式，`0` 使用原生默认值 continuous（连续固定） |
| `sateph` | 星历选项，`0` 自动选择精密星历或广播星历 |
| `nf` | 频点数，`0` 使用原生默认值 `2` |
| `minfix` | 首次固定需连续通过的历元数，`0` 使用原生默认值 `5` |
| `warmupMin` | 预热时长，单位分钟，`0` 关闭；预热期仅驱动滤波，不计入报告 |

以上新增解算选项在 Java 中默认均为 `0`，由 LMonitor 应用上述默认行为；非零选项值按原生算法的定义传入。

`MonitorServiceImpl` 会做以下转换：

- 校验 `roverBytes`、`baseBytes` 非空。
- 将 `timeStart/timeEnd` 转成原生库需要的 `double[6]`。
- 将 `navSys` 字符串拆分求和后传给原生库。
- `brdcBytes` 非空时自动补 `0` 结尾，避免底层按 C 字符串读取星历时越界。
- 将数据流写入 JNA native memory，并填入 `MonitorStreamInfo` 的指针和长度字段。
- 将 `precBytes` 映射为 `precBuf/precLen`，将七个解算选项原样传入对应字段，`warmupMin` 对应原生 `warmup_min`。

## 直接调用原生库：MonitorLibrary

如果不使用 Spring，也可以直接调用 JNA 封装。

```java
import com.navfirst.lmonitor.lib.library.MonitorLibrary;
import com.navfirst.lmonitor.lib.library.MonitorStreamInfo;
import com.navfirst.lmonitor.lib.utils.TimeUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class DirectCallDemo {

    public static void main(String[] args) throws Exception {
        Path brdcPath = Path.of("/path/to/BRDM1400.rnx");
        byte[] brdc = Files.isRegularFile(brdcPath) ? Files.readAllBytes(brdcPath) : null;
        byte[] rover = Files.readAllBytes(Path.of("/path/to/XPJZ01.2026140binRTCM3"));
        byte[] base = Files.readAllBytes(Path.of("/path/to/XPJZ02.2026140binRTCM3"));
        Path precPath = Path.of("/path/to/precise.sp3");
        byte[] prec = Files.isRegularFile(precPath) ? Files.readAllBytes(precPath) : null;

        MonitorStreamInfo info = MonitorStreamInfo.builder()
                .calMode(3)
                .es(TimeUtils.parseEpoch("2026/05/20 00:00:00"))
                .ee(TimeUtils.parseEpoch("2026/05/20 01:00:00"))
                .ti(0)
                .vrsMode(0)
                .navsys(45)
                .solstatic(1)
                .fixThresh(0.75)
                .rb(new double[]{0D, 0D, 0D})
                .outfile("")
                .ionoopt(0)
                .tropopt(0)
                .armode(0)
                .sateph(0)
                .nf(0)
                .minfix(0)
                .warmupMin(0)
                .build();

        info.setBrdc(appendZeroTerminator(brdc));
        info.setRover(rover);
        info.setBase(base);
        info.setPrec(prec);

        String result = MonitorLibrary.INSTANCE.startLMonitor(info, "/path/to/license.lic");
        String[] lines = result == null ? new String[]{"", ""} : result.split("\\R", 2);

        String solBuf = lines.length > 0 ? lines[0] : "";
        String errMsg = lines.length > 1 ? lines[1] : "";

        System.out.println("solBuf = " + solBuf);
        System.out.println("errMsg = " + errMsg);
    }

    private static byte[] appendZeroTerminator(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        if (bytes.length > 0 && bytes[bytes.length - 1] == 0) {
            return bytes;
        }
        return Arrays.copyOf(bytes, bytes.length + 1);
    }
}
```

直接调用时要注意：

- `MonitorStreamInfo` 的字段顺序必须和原生库结构体一致，不要随意调整。
- `setBrdc/setRover/setBase/setPrec` 会复制 Java `byte[]` 到 JNA native memory，并设置对应指针和长度；传 `null` 或空数组时清空指针并将长度设为 `0`。
- `brdc` 可以为空；非空时建议补一个 `0` 结尾。通过 `MonitorService` 调用时会自动处理，直接调用时需要自己处理。
- `prec` 可以为空；`setPrec` 按 SP3 原始字节设置指针和长度，无需补 `0` 结尾。
- `startLMonitor` 当前在 Java 中映射为 `String` 返回值，调用方不需要手动解析指针。

## 原生结构体映射

Java 的 `MonitorStreamInfo` 对应原生库的 `LMonitorStreamInfo`：

```c
typedef struct LMonitorStreamInfo {
    int32_t calMode;
    double es[6];
    double ee[6];
    int32_t ti;
    int32_t vrsMode;
    int32_t navsys;
    int32_t solstatic;
    double fixThresh;
    double rb[3];
    const char* outfile;
    const uint8_t* brdc_buf;
    uint64_t brdc_len;
    const uint8_t* rover_buf;
    uint64_t rover_len;
    const uint8_t* base_buf;
    uint64_t base_len;
    const uint8_t* prec_buf;
    uint64_t prec_len;
    int32_t ionoopt;
    int32_t tropopt;
    int32_t armode;
    int32_t sateph;
    int32_t nf;
    int32_t minfix;
    double warmup_min;
} LMonitorStreamInfo;
```

对应 Java 字段：

| Java 字段 | 原生字段 | 说明 |
| --- | --- | --- |
| `calMode` | `calMode` | 解算模式 |
| `es` | `es[6]` | 开始时间：年、月、日、时、分、秒 |
| `ee` | `ee[6]` | 结束时间：年、月、日、时、分、秒 |
| `ti` | `ti` | 采样间隔 |
| `vrsMode` | `vrsMode` | VRS 模式 |
| `navsys` | `navsys` | 卫星系统聚合值 |
| `solstatic` | `solstatic` | 输出模式 |
| `fixThresh` | `fixThresh` | 最小固定率 |
| `rb` | `rb[3]` | 基准站坐标 |
| `outfile` | `outfile` | 输出文件路径 |
| `brdcBuf/brdcLen` | `brdc_buf/brdc_len` | 广播星历数据 |
| `roverBuf/roverLen` | `rover_buf/rover_len` | 监测站数据 |
| `baseBuf/baseLen` | `base_buf/base_len` | 基准站数据 |
| `precBuf/precLen` | `prec_buf/prec_len` | 精密星历数据（SP3） |
| `ionoopt` | `ionoopt` | 电离层选项 |
| `tropopt` | `tropopt` | 对流层选项 |
| `armode` | `armode` | 模糊度固定模式 |
| `sateph` | `sateph` | 星历选项 |
| `nf` | `nf` | 频点数 |
| `minfix` | `minfix` | 首次固定需连续通过的历元数 |
| `warmupMin` | `warmup_min` | 预热时长（分钟） |

类型映射：`int32_t` → Java `int`，`uint64_t` → Java `long`（数据长度使用非负值），`double` → Java `double`，数据指针 → JNA `Pointer`，`const char* outfile` → Java `String`。数组 `es/ee/rb` 必须分别保持长度 `6/6/3`。

在本项目内置原生库支持的 64 位平台上，结构体大小为 **256 字节**：`precBuf` 偏移 `208`、`precLen` 偏移 `216`、`ionoopt` 偏移 `224`、`warmupMin` 偏移 `248`。Java 字段采用驼峰命名，通过固定字段顺序与 C 结构体对应。

## 返回结果

`startLMonitor` 返回两行文本：

```text
<solBuf>
<errMsg>
```

长基线原生库当前返回的 `solBuf` 包含 30 个以空白分隔的字段，顺序为：

```text
startDate startTime endDate endTime dposMax dposAvg dposStd fixedRate roverEpochRate baseEpochRate E N U X Y Z B L H solStatus solutionType satNum isMoved roverSample baseSample roverObsNum baseObsNum fileStatus navStatus offTime
```

字段含义：

| 字段 | 含义 | 单位/格式 |
| --- | --- | --- |
| `E N U` | 相对基准站的东、北、天坐标 | 米 |
| `X Y Z` | 监测站 ECEF 地心地固坐标 | 米 |
| `B L H` | 监测站 WGS84 大地纬度、经度、椭球高 | 度、度、米 |
| `isMoved` | 长基线结果中的测站移动标志 | 整数 |
| `offTime` | 解算耗时 | 例如 `0.3s` |

`X/Y/Z/B/L/H` 由原生库根据传入的基准站 ECEF `rb={baseX,baseY,baseZ}` 计算。`baseX/baseY/baseZ` 全部为 `0` 或未设置时，六个绝对坐标字段均为 `0`；`NONE` 结果也保持为 `0`。`B/L` 为十进制度，`H` 为 WGS84 椭球高。Java SDK 会保留 `isMoved`，不会把它误解析为采样率。

为已有任务设置基准站坐标，并在回调中读取绝对坐标：

```java
task.setBaseX(-1647115.6013); // ECEF X，米
task.setBaseY(4602291.3375);  // ECEF Y，米
task.setBaseZ(4085428.6662);  // ECEF Z，米
monitorService.setHandlerData(data -> {
    System.out.printf("XYZ(m): %.4f %.4f %.4f%n", data.getX(), data.getY(), data.getZ());
    System.out.printf("BLH(deg,deg,m): %.9f %.9f %.4f%n", data.getB(), data.getL(), data.getH());
    System.out.println("isMoved = " + data.getIsMoved());
});
monitorService.startMonitor(task, "/path/to/license.lic");
```

坐标转换由 Go 封装完成，Java SDK 负责解析。有效 `Fixed` / `Float` 结果的 ENU 全零时，监测站 XYZ 等于传入的基准站 XYZ；基准站坐标无效或转换失败时，六个绝对坐标字段为 `0`，具体原因通过 `errMsg` 返回。应结合解状态和告警判断结果有效性。

示例：

```text
2026/07/03 02:00:00 2026/07/03 03:00:00 0.1488 0.0468 0.0205 0.9915 0.9833 0.9833 36.9577 -5.6954 7.4100 -1647153.5438 4602287.6750 4085429.0790 40.077629137 109.692231002 1311.3510 Fixed 1 29 1 15 15 236 236 0 0 1.1s
```

为了兼容旧版原生库，解析器仍接受历史 23 列和 24 列结果；历史结果没有绝对坐标时，`X/Y/Z/B/L/H` 使用 `0`，24 列结果中的 `isMoved` 仍按其原字段解析，23 列结果的 `isMoved` 为 `0`。支持连续空格和 Tab；不支持的字段数量使同步解析返回 `null`，回调解析不触发回调。

通过 `MonitorService` 调用时，`MonitorDataService` 会把 `solBuf` 解析成 `MonitorData`，再通过 `HandlerDataInterface` 回调出去。`MonitorData` 新增 `X/Y/Z/B/L/H` 和 `isMoved` 属性；`gpsTime/lastObsTime` 均取返回的结束时间。

## 测试

测试类：

```text
src/test/java/com/navfirst/lmonitor/lib/services/impl/MonitorDataServiceImplTests.java
src/test/java/com/navfirst/lmonitor/lib/LmonitorJavaLibApplicationTests.java
```

`MonitorDataServiceImplTests` 覆盖 30 列长基线结果、`X/Y/Z/B/L/H`、`isMoved`、零坐标和多空白解析；`LmonitorJavaLibApplicationTests` 覆盖本地原生库加载和可选的真实数据解算。

当前 `testMonitor` 使用如下路径：

```text
/Users/wfu/Downloads/GSSK01.2026210binRTCM3
/Users/wfu/Downloads/license.lic
```

运行：

```bash
./mvnw test
```

结构体测试验证 256 字节布局、字段偏移及精密星历缓冲区，服务测试验证解算选项和数据流传参。

`testMonitor` 是本地原生库集成测试，当前监测站和基准站使用同一份 RTCM3 样例，时间范围为 `2026/07/29 04:00:00` 至 `05:00:00`。如果 RTCM3 或 license 文件不存在，测试会跳过。该 RTCM3 文件不能作为 RINEX 广播星历传入；测试不传外部广播星历和精密星历。

## 常见问题

### 找不到 libLMonitor

确认当前系统对应的原生库在 classpath 中，或者指定：

```bash
-Djna.library.path=/path/to/native/lib/dir
```

### 解算返回错误信息

优先检查：

- license 是否有效
- 如果传入了 `brdcBytes`，确认它是否为对应日期的广播星历
- `roverBytes/baseBytes` 是否为同一时间段的数据流
- `timeStart/timeEnd` 是否覆盖数据流时间范围
- `navSys` 是否包含需要的卫星系统

### 直接调用时没有结果

确认已调用必需数据流：

```java
info.setRover(...);
info.setBase(...);
```

`info.setBrdc(...)` 和 `info.setPrec(...)` 可选。只设置标量字段但不设置 `rover/base` 数据流时，原生库没有实际观测数据可解算。
