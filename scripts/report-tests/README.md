# LMonitor 测试报告复现说明

历史报告测量对象为 Git `f46b42f` 加 2026-09-10 工作区修订，源文件哈希见 `docs/test-evidence/2026-09-10/updated-source-manifest.json`。该目录的 `updated-*` 是当日修订后的结果，其他证据为当日修订前记录。原生库版本为 1.0.0。当前脚本已同步到 2026-09-18 的 30 列协议，运行结果写入 `target/report-work`，不覆盖历史测量。

## 环境与构建

需要 JDK 20+、Maven Wrapper、平台原生库。实测环境为 JDK 21.0.9、Maven 3.9.16、macOS arm64。

```sh
./mvnw clean test
./mvnw -DskipTests package
mkdir -p target/report-work
./mvnw -DskipTests test-compile dependency:build-classpath \
  -Dmdep.outputFile=target/report-work/classpath.txt
```

2026-09-18 完整回归 6 项通过，其中新增 3 项结果解析断言测试。原有集成测试的同站样例返回 Float、固定率 0，因此不能据 JUnit 绿色状态判定解算质量。默认制品是 Spring Boot 可执行 JAR，作为 SDK 分发应关闭 repackage 或单独发布普通 JAR。开发安装命令：

```sh
./mvnw -DskipTests -Dspring-boot.repackage.skip=true clean install
```

## Java 契约检查

```sh
REPORT_CP="target/classes:$(cat target/report-work/classpath.txt)"
javac -proc:none -cp "$REPORT_CP" -d target/report-work \
  scripts/report-tests/JavaContractProbe.java
java -cp "target/report-work:$REPORT_CP" JavaContractProbe \
  target/report-work/java-contract.json
```

19 项断言检查时间、任务校验、内存复制、人工 23/24/30 项文本、回调和状态映射，另记录 JNA 尺寸及偏移。任一断言失败退出码为 1。只读取原生版本，不调用解算。最新尺寸 256 B，25 个字段，与 C 参考布局一致。人工文本不代表所有原生输出格式均兼容。

## GNSS 解算采集

执行下面脚本，指定数据目录、当前机器许可证和一个空的输出目录。Python 需要标准库；Java 依赖沿用构建产生的 classpath。

```sh
python3 scripts/report-tests/run_checks.py \
  --data /path/to/gnss \
  --license /path/to/license.lic \
  --output target/report-work/new-evidence
```

输入目录需包含：

- `nav/2026/BRDM2490.rnx`。
- `raw/2026/249/12` 和 `13` 下的 `PSYCMM0010`、`PSYCMM0009`、`PSYCMM129` 对应 `.2026249binRTCM3` 文件。
- 第 249 天从 00 时起、监测站和长基线基站足够连续小时文件，用于各截取约 2.5 MB 净 RTCM3。

`run_checks.py` 核对去分块封装的边界、执行 CRC24Q 和帧审计，并将夹具存到 `target/report-work`。场景包括：3 个串行站对窗口、长基线重复 4 次、1/15/30 分钟窗口、双线程 2 轮、双站约 5 MB 重复 3 次。各场景独立子 JVM，设定超时；JSON 记录每任务回调数、结果和异常。日志中的 UUID 脱敏。

采集程序记录事实，不把质量告警伪装成通过。验收条件需另行判断：恰好一次回调、无异常、有限 ENU、错误为空、观测和星历状态为 0、最终 Fixed 且固定率至少 0.75。目录已有内容时脚本拒绝覆盖。

`callMs` 从 `startMonitor` 前到同步返回后，不含文件读取和 JVM/库加载。重复调用首轮单列；后 3 次或 2 次统计。双线程只验证样本结果一致，不用于最佳并发或线程安全承诺。净 RTCM3 字节数包括帧头与 CRC，不包括传输封装和独立导航文件。

## ABI 与历史结果

`native-abi-reference.h` 从本机 `lmonitor-go-lib/main.go` 提取，首行记录源文件 SHA-256。使用 `sizeof(LMonitorStreamInfo)` 和 `offsetof(LMonitorStreamInfo, 字段名)` 可复现 `native-abi-layout.txt`；该源码参考不能代替交付二进制与头文件的配套保证。

修订前 Java 结构仅 208 B，完整回归曾原生崩溃。最新结构为 256 B，相关字段已经传入，完整回归及独立样本正常退出。两轮证据分别保留，不混淆测试对象。

默认可执行 JAR 与同次 `.jar.original` 的导入对照源文件：

```java
import com.navfirst.lmonitor.lib.domains.MonitorTask;
class DependencyCompileProbe { MonitorTask task; }
```

分别以两份制品作为 `javac -proc:none -cp` 参数，默认 JAR 导入失败，普通 JAR 成功。见 `dependency-compile.json`。最新 pom.xml 没有调整 repackage，因此普通 SDK 制品分发仍需处理。

本轮未执行独立真值精度、真实位移平台、精密星历与非默认参数、长期内存及 Linux 算法验证。更换源码或原生库后应写入新证据目录并重新测量。
