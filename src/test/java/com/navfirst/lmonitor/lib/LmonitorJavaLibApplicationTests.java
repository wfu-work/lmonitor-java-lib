package com.navfirst.lmonitor.lib;

import com.navfirst.lmonitor.lib.domains.MonitorTask;
import com.navfirst.lmonitor.lib.services.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LmonitorJavaLibApplicationTests {

    private static final Path BRDC_PATH = Path.of("/Users/wfu/Downloads/GSSK01.2026210binRTCM3");
    private static final Path ROVER_PATH = Path.of("/Users/wfu/Downloads/GSSK01.2026210binRTCM3");
    private static final Path BASE_PATH = Path.of("/Users/wfu/Downloads/GSSK01.2026210binRTCM3");
    private static final String LICENSE_PATH = "/Users/wfu/Downloads/license.lic";

    private final MonitorService monitorService;

    @Test
    void testVersion() {
        String version = this.monitorService.getVersion();
        log.info("算法版本：{}", version);
    }

    @Test
    void testUUid() {
        String uuid = this.monitorService.getUuid();
        log.info("许可证唯一标识：{}", uuid);
    }

    @Test
    void testMonitor() throws IOException {
        assumeTrue(Files.isRegularFile(ROVER_PATH), "缺少监测站数据流：" + ROVER_PATH);
        assumeTrue(Files.isRegularFile(BASE_PATH), "缺少基准站数据流：" + BASE_PATH);
        assumeTrue(Files.isRegularFile(Path.of(LICENSE_PATH)), "缺少 license：" + LICENSE_PATH);

        this.monitorService.setHandlerData(monitorData -> log.info("解算结果：{}", monitorData));
        byte[] brdcBytes = Files.isRegularFile(BRDC_PATH) ? Files.readAllBytes(BRDC_PATH) : null;
        MonitorTask monitorTask = MonitorTask.builder()
                .rtMode(3)
                .timeStart("2026/07/29 04:00:00")
                .timeEnd("2026/07/29 05:00:00")
                .sample(0)
                .vrs(0)
                .filterPeriod(7200)
                .processInterval(600)
                .roverName("GSSK01")
                .baseName("GSSK01")
                .brdcBytes(brdcBytes)
                .roverBytes(Files.readAllBytes(ROVER_PATH))
                .baseBytes(Files.readAllBytes(BASE_PATH))
                .outMode(1)
                .minFixedRate(0.75)
                .navSys("1,4,8,32")
                .bds(1)
                .build();
        this.monitorService.startMonitor(monitorTask, LICENSE_PATH);
    }

}
