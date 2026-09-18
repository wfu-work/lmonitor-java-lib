# LMonitor Java SDK 文档

基线：SDK 1.0.0，文档修订 2026-09-18；长基线结果协议已同步到当前原生库的 30 列输出。版式参考相邻 `dmonitor-java-lib/docs` 中的对应文档。

| 文档 | Word | PDF |
| --- | --- | --- |
| SDK 开发指南 | [开发指南](LMonitor-Java-SDK开发指南.docx) | [开发指南 PDF](LMonitor-Java-SDK开发指南.pdf) |
| SDK 测试报告（2026-09-10 历史测量） | [测试报告](LMonitor-Java-SDK测试报告.docx) | [测试报告 PDF](LMonitor-Java-SDK测试报告.pdf) |

另附 [Markdown 报告及 2026-09-18 协议适配复测](LMonitor-Java-SDK测试报告.md)、[历史证据](test-evidence/2026-09-10) 和 [复现说明](../scripts/report-tests/README.md)。

当前代码验证：`./mvnw test` 共 6 项通过；新增 30 列结果解析测试，覆盖 `X/Y/Z/B/L/H`、`isMoved`、零坐标、连续空白和不支持字段数。长基线结果字段为 `E N U X Y Z B L H`，未提供基准站 ECEF 或 `NONE` 状态时六个绝对坐标字段为 0。小时窗口固定率、绝对精度、真实位移响应及 Linux 运行仍需按业务数据单独验收。
