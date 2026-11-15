# 第三方平台经营测试计划

## Feature scope

验证独立第三方平台目录、订单、房态、统计与经营者权限；小程序订单及统计不合并。真实数据和线上服务不作为测试目标。

## Module breakdown

- 管理后台四个页面及其 API 封装。
- `ExternalController` 的 HTTP 路由、请求校验和鉴权。
- `ExternalServiceImpl` 的目录、订单、状态、冲突、财务分摊。
- MySQL 三表、唯一约束和跨事务房间锁。
- 现有小程序订单支付相关自动测试。

## Test objectives

保证关键路径、边界、失败和重复操作可预测，并检查第三方数据修改不会写入小程序订单。识别无法通过本轮自动测试证明的回归风险。

## Test strategy

- **Unit Test**：整数分分摊、跨月与维度去重、服务端权限。
- **API Test**：以 MockMvc 测试 `/external/platforms`、`/anchors`、`/orders`、`/orders/{id}`、状态、`/calendar`、`/statistics`，覆盖参数校验、业务错误、异常格式及方法分派。
- **Integration Test**：环境变量启用 Spring Boot + MockMvc，使用临时 Docker MySQL，不访问现有项目数据库；检查订单生命周期、平台改名、备注修改和数据库隔离。
- **Boundary Test**：空字段、零人数、三位小数、跨月余分、日期相邻及同房日期重叠。
- **Negative Test**：无权用户、错误日期、缺失参数、无效状态、重复平台订单号及非法状态跳转。
- **Idempotency Test**：同平台重复订单号拒绝，重复状态请求拒绝且不重复产生状态变化；脚本重复执行结果见报告。
- **Concurrency Test**：两线程为同房同日同时录单，验证至多一个提交。
- **Regression Test**：执行 Maven 原有订单与支付测试；浏览器及完整小程序端到端测试需另行验证。
