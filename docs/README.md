# LMonitor Java SDK 文档

基线：SDK 1.0.0，Git `f46b42f` 加 2026-09-10 工作区修订快照。版式参考相邻 `dmonitor-java-lib/docs` 中的对应文档。

| 文档 | Word | PDF |
| --- | --- | --- |
| SDK 开发指南 | [开发指南](LMonitor-Java-SDK开发指南.docx) | [开发指南 PDF](LMonitor-Java-SDK开发指南.pdf) |
| SDK 测试报告 | [测试报告](LMonitor-Java-SDK测试报告.docx) | [测试报告 PDF](LMonitor-Java-SDK测试报告.pdf) |

另附 [Markdown 报告](LMonitor-Java-SDK测试报告.md)、[本轮证据](test-evidence/2026-09-10) 和 [复现说明](../scripts/report-tests/README.md)。

最新验证为条件通过：完整 JUnit 回归 3 项通过，Java 契约检查 18 项通过；长基线约 5 MB 样本返回 Fixed、固定率 95.52%。小时窗口仍有固定率不足告警，绝对精度、真实位移响应及 Linux 运行待验。初始结构体崩溃已在最新快照中不再复现，历史记录见报告附录 B。
