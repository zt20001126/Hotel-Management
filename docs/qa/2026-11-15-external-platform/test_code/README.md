# 可执行测试的位置

项目使用 Maven 约定目录，测试源代码在：

- `backend/src/test/java/com/homestay/module/external/ExternalApiTest.java`
- `backend/src/test/java/com/homestay/module/external/ExternalAuthorizationTest.java`
- `backend/src/test/java/com/homestay/module/external/ExternalIntegrationTest.java`
- `backend/src/test/java/com/homestay/module/external/ExternalServiceTest.java`
- `backend/src/test/java/com/homestay/module/external/service/NightlyAllocationTest.java`

在 `backend` 运行 `mvn test`；其中 MySQL 集成测试要求设置 `EXTERNAL_INTEGRATION_TEST=true`、`DATABASE_URL` 和 `JWT_SECRET`，必须使用隔离且已初始化的测试库。未设置时集成测试被跳过。不要指向生产数据库。

无头浏览器冒烟测试在 `frontend/admin/test/external-calendar-smoke.cjs`，在 `frontend/admin` 下运行 `node test/external-calendar-smoke.cjs`。使用本机 Chrome、临时浏览器配置和模拟 API，可通过 `CHROME_PATH` 指定 Chrome 程序位置，不连接业务数据。
