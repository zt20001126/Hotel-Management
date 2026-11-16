# 测试代码位置

- `backend/src/test/java/com/homestay/module/external/MiniProgramAggregationIntegrationTest.java`
- `backend/src/test/java/com/homestay/module/external/MiniProgramEndToEndIntegrationTest.java`
- `backend/src/test/java/com/homestay/module/external/ExternalPriceIntegrationTest.java`
- `backend/src/test/java/com/homestay/module/external/service/NightlyAllocationTest.java`
- `frontend/admin/test/external-calendar-smoke.cjs`

在 `backend` 运行 `mvn test`。数据库集成测试需设置 `EXTERNAL_INTEGRATION_TEST=true`、隔离且已初始化的测试库 `DATABASE_URL`、测试 `JWT_SECRET`。不要使用生产数据库。浏览器测试在 `frontend/admin` 运行 `node test/external-calendar-smoke.cjs`，使用本地 Chrome 与模拟 API。
