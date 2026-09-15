# MeetHere 预约模块 AI 测试

启动 MySQL 和 MeetHere 后，在项目根目录运行指定用例。每条用例都会自动打开 Chrome，并在页面上显示测试结果。

```bash
.\mvnw.cmd -Dtest=AiReservation001Test test
```

测试脚本位于 `src/test/java/com/meethere/aireservation`。部分用例用于定位缺陷，断言失败表示发现了与预期不一致的行为，相关订单会保留供检查。
