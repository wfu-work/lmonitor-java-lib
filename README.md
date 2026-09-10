# lmonitor-java-lib

`lmonitor-java-lib` 是 `libLMonitor` 原生库的 Java/JNA 封装，提供 GNSS 长基线监测解算能力。

当前工程内置的原生库：

- `src/main/resources/linux-x86-64/libDMonitor.so`
- `src/main/resources/linux-aarch64/libDMonitor.so`
- `src/main/resources/darwin-aarch64/libDMonitor.dylib`

核心调用入口有两层：

- 推荐：通过 Spring 注入 `MonitorService`
- 直接：通过 JNA 调用 `MonitorLibrary.INSTANCE.startGMonitor(...)`

## 运行要求

- JDK 20 或以上
- Spring Boot 3.2.x
- JNA 5.12.x
- 当前运行系统架构需要有对应的 `libDMonitor` 原生库
- 调用解算时必须提供 license 路径

如果在 IDE 或外部工程中出现 `UnsatisfiedLinkError`，优先检查：

- `libDMonitor.so` 或 `libDMonitor.dylib` 是否在 classpath 资源中
- 当前机器架构是否和资源目录匹配，例如 `linux-x86-64`、`linux-aarch64`、`darwin-aarch64`
- 必要时手动指定：

```bash
-Djna.library.path=/path/to/native/lib/dir
```

## 原生库方法

JNA 接口定义在：

```java
import com.navfirst.lmonitor.lib.library.MonitorLibrary;

MonitorLibrary
```

当前暴露的方法：

```java
String getGVersion();

String getUuid();

String startGMonitor(MonitorStreamInfo monitorInfo, String license);
```

说明：

- `getGVersion()`：获取算法库版本。
- `getUuid()`：获取当前机器唯一标识，用于申请 license。
- `startGMonitor(...)`：调用原生库执行解算。

`startGMonitor` 返回值格式为：

```text
<solBuf>
<errMsg>
```

第一行是解算结果，第二行是错误信息。`MonitorService` 会自动拆分这两行并转换为 `MonitorData`。

## 推荐调用方式：MonitorService

在 Spring 环境中直接注入：

```java
import domains.com.navfirst.lmonitor.lib.MonitorTask;
import services.com.navfirst.lmonitor.lib.MonitorService;
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

重要字段：

| 字段 | 说明 |
| --- | --- |
| `rtMode` | 解算模式：`0` SPP，`2` 动态，`3` 静态 |
| `timeStart` | 开始时间，支持 `yyyy/MM/dd HH:mm:ss` 和 `yyyy-MM-dd HH:mm:ss` |
| `timeEnd` | 结束时间，支持同上格式 |
| `sample` | 采样间隔；通常传 `0` |
| `vrs` | 是否 VRS：`0` 否，`1` 是 |
| `outMode` | 输出模式：`0` 历元解，`1` 单一解 |
| `minFixedRate` | 最小固定率，例如 `0.75` |
| `navSys` | 卫星系统聚合项，例如 `1,4,8,32` |
| `bds` | 北斗频段开关；为 `0` 时会从 `navSys` 中移除 `32` |
| `baseX/baseY/baseZ` | 基准站 ECEF 坐标，可不传，默认 `0` |
| `outFile` | 底层算法输出文件路径，可不传 |

`MonitorServiceImpl` 会做以下转换：

- 校验 `roverBytes`、`baseBytes` 非空。
- 将 `timeStart/timeEnd` 转成原生库需要的 `double[6]`。
- 将 `navSys` 字符串拆分求和后传给原生库。
- `brdcBytes` 非空时自动补 `0` 结尾，避免底层按 C 字符串读取星历时越界。
- 将数据流写入 JNA native memory，并填入 `MonitorStreamInfo` 的指针和长度字段。

## 直接调用 SO：MonitorLibrary

如果不使用 Spring，也可以直接调用 JNA 封装。

```java
import library.com.navfirst.lmonitor.lib.MonitorLibrary;
import library.com.navfirst.lmonitor.lib.MonitorStreamInfo;
import utils.com.navfirst.lmonitor.lib.TimeUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class DirectCallDemo {

    public static void main(String[] args) throws Exception {
        Path brdcPath = Path.of("/path/to/BRDM1400.rnx");
        byte[] brdc = Files.isRegularFile(brdcPath) ? Files.readAllBytes(brdcPath) : null;
        byte[] rover = Files.readAllBytes(Path.of("/path/to/XPJZ01.2026140binRTCM3"));
        byte[] base = Files.readAllBytes(Path.of("/path/to/XPJZ02.2026140binRTCM3"));

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
                .build();

        info.setBrdc(appendZeroTerminator(brdc));
        info.setRover(rover);
        info.setBase(base);

        String result = MonitorLibrary.INSTANCE.startGMonitor(info, "/path/to/license.lic");
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
- `setBrdc/setRover/setBase` 会把 Java `byte[]` 写入 JNA native memory，并设置对应指针和长度。
- `brdc` 可以为空；非空时建议补一个 `0` 结尾。通过 `MonitorService` 调用时会自动处理，直接调用时需要自己处理。
- `startGMonitor` 当前在 Java 中映射为 `String` 返回值，调用方不需要手动解析指针。

## 原生结构体映射

Java 的 `MonitorStreamInfo` 对应原生库的 `GMonitorStreamInfo`：

```c
typedef struct GMonitorStreamInfo {
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
} GMonitorStreamInfo;
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

## 返回结果

`startGMonitor` 返回两行文本：

```text
<solBuf>
<errMsg>
```

`solBuf` 字段顺序：

```text
endDate endTime lastGpsDate lastGpsTime dposMax dposAvg dposStd fixedRate roverEpochRate baseEpochRate E N U solStatus solutionType satNum roverSample baseSample roverObsNum baseObsNum fileStatus navStatus navNum offTime
```

通过 `MonitorService` 调用时，`MonitorDataService` 会把 `solBuf` 解析成 `MonitorData`，再通过 `HandlerDataInterface` 回调出去。

## 测试

测试类：

```text
src/test/java/com/navfirst/dmonitor/lib/DmonitorJavaLibApplicationTests.java
```

当前 `testMonitor` 使用如下路径：

```text
/Users/wfu/Downloads/nav/2026/BRDM1400.rnx
/Users/wfu/Downloads/raw/2026/140/00/XPJZ01.2026140binRTCM3
/Users/wfu/Downloads/raw/2026/140/00/XPJZ02.2026140binRTCM3
/Users/wfu/Downloads/license-mini.lic
```

运行：

```bash
./mvnw test
```

如果 RTCM3 或 license 文件不存在，`testMonitor` 会跳过，不会误调用原生库。BRDC 星历文件不存在时会传空。

## 常见问题

### 找不到 libDMonitor

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

`info.setBrdc(...)` 可选。只设置标量字段但不设置 `rover/base` 数据流时，原生库没有实际观测数据可解算。`brdc` 数据流可为空。
