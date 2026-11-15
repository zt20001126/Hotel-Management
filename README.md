# 芊浔民宿业务管理系统

一体化民宿业务管理系统，包含管理后台（Web）和宾客端（微信小程序）。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0+
- **安全**: JWT 认证
- **构建**: Maven 3.6+

### 前端 - 管理后台
- **框架**: Vue 3.4
- **UI组件**: Element Plus 2.4
- **构建工具**: Vite 5.0
- **HTTP客户端**: Axios
- **图表**: ECharts 5.4
- **状态管理**: Pinia

### 前端 - 宾客端
- **平台**: 微信小程序

## 项目结构

```
homestay-business-management/
├── backend/                    # 后端项目
│   ├── src/main/java/com/homestay/
│   │   ├── common/            # 通用模块
│   │   ├── config/            # 配置类
│   │   ├── context/           # 上下文
│   │   ├── interceptor/       # 拦截器
│   │   └── module/            # 业务模块
│   │       ├── system/        # 系统管理(员工/宾客/日志/配置)
│   │       ├── room/          # 房间管理
│   │       ├── order/         # 订单管理
│   │       ├── payment/       # 支付管理
│   │       ├── inventory/     # 库存管理
│   │       ├── pos/           # POS消费
│   │       ├── cleaning/      # 清洁任务
│   │       └── statistics/    # 统计分析
│   ├── sql/                   # 数据库脚本
│   └── pom.xml
├── frontend/
│   ├── admin/                 # 管理后台
│   │   ├── src/
│   │   │   ├── api/          # API接口
│   │   │   ├── layout/       # 布局组件
│   │   │   ├── router/       # 路由配置
│   │   │   ├── stores/       # 状态管理
│   │   │   ├── utils/        # 工具函数
│   │   │   └── views/        # 页面组件
│   │   └── package.json
│   ├── guest-web/            # 宾客Web端
│   │   ├── src/
│   │   │   ├── api/          # API接口
│   │   │   ├── router/       # 路由配置
│   │   │   ├── stores/       # 状态管理
│   │   │   ├── utils/        # 工具函数
│   │   │   ├── views/        # 页面组件
│   │   │   └── styles/       # 样式文件
│   │   └── package.json
│   └── miniprogram/          # 微信小程序
│       ├── pages/            # 页面
│       ├── app.js
│       ├── app.json
│       └── app.wxss
└── README.md
```

## 功能模块

### 宾客端功能
- **房间查询**: 按日期、人数、偏好查询可用房间
- **房间详情**: 查看房型、价格、设施、照片等信息
- **在线预订**: 选择房型、填写信息、支付保证金
- **订单管理**: 查看订单状态、取消订单
- **个人中心**: 查看/修改个人信息、历史订单

#### 宾客端平台
- **Web端**: 官方网站，提供完整的自助服务功能
- **微信小程序**: 移动端便捷预订

### 管理端功能

#### 🔹 民宿经营者（最高权限）
- 员工管理：增删改员工账号、分配角色
- 房间与价格管理：设置房型、设施、动态调价
- 经营分析：查看营收、成本、利润报表
- 系统设置：配置保证金规则、取消政策
- 权限审计：查看操作日志

#### 🔹 前台员工（中等权限）
- 入住办理：为宾客办理入住、分配房间
- 结账退房：生成账单、押金抵扣、退款处理
- 订单管理：创建/修改/取消订单
- POS消费录入：记录宾客在店消费
- 房态查看：实时查看房间状态

#### 🔹 房务员工（基础权限）
- 房态更新：标记房间清洁状态
- 清洁任务：查看待清洁房间、完成任务
- 物资消耗：记录清洁用品消耗

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- 微信开发者工具（小程序开发）

### Docker 部署（后端、MySQL、Redis）

项目根目录提供 `compose.yaml`，可通过 Docker Compose 启动 Spring Boot 后端、MySQL 和 Redis。首次克隆项目且本地还没有 `.env` 时，复制环境变量模板并填写配置：

```powershell
Copy-Item .env.example .env
```

编辑 `.env`，至少设置完整的 `DATABASE_URL`、MySQL root 初始化密码、`REDIS_URL` 和 JWT 密钥。数据库 URL 采用 MySQL JDBC 格式，并在查询参数中包含 `user` 和 `password`。Redis URL 格式为 `redis://:密码@redis:6379`，密码使用字母、数字或下划线。微信小程序登录还需要填写 `WECHAT_MINI_PROGRAM_APP_ID` 和 `WECHAT_MINI_PROGRAM_APP_SECRET`。`.env` 含敏感信息，不要提交到 Git。

启动服务：

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

后端 API 地址为 `http://localhost:8080/api`。MySQL 初始化时会从 `DATABASE_URL` 解析数据库名和应用账号，并导入 `backend/sql/homestay_db.sql`；该脚本只会在 MySQL 数据卷首次初始化时执行。MySQL、Redis 和上传文件使用 Docker 命名卷持久化。`docker compose down` 会停止并移除容器，但保留这些数据卷。

MySQL 和 Redis 端口只绑定到服务器/开发机的本机回环地址；本机访问 MySQL 使用 `127.0.0.1:3307`，容器内仍使用 `mysql:3306`。小程序正式发布前，需要为后端配置 HTTPS 域名和反向代理，并在微信公众平台设置对应的服务器域名。当前后端代码尚未集成 Redis 客户端，因此 Redis 容器会启动，但业务代码暂时不会访问它。

### 数据库初始化

1. 创建数据库并执行SQL脚本：
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS qianxun CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql -u root -p qianxun < backend/sql/homestay_db.sql
```

2. 本项目默认数据库为 `qianxun`，Docker 部署时数据库连接由项目根目录 `.env` 中的 `DATABASE_URL` 提供：
```yaml
DATABASE_URL=jdbc:mysql://mysql:3306/qianxun?user=qianxun&password=qianxun
```

### 启动后端

```bash
docker compose up -d --build backend mysql redis
```

后端启动后访问：
- API地址: http://localhost:8080/api
- Swagger文档: http://localhost:8080/api/swagger-ui.html

### 启动管理后台

```bash
cd frontend/admin
npm install
npm run dev
```

管理后台访问: http://localhost:3000

### 启动宾客Web端

```bash
cd frontend/guest-web
npm install
npm run dev
```

宾客Web端访问: http://localhost:3001

### 微信小程序

1. 使用微信开发者工具打开 `frontend/miniprogram` 目录
2. 修改 `app.js` 中的 `baseUrl` 为实际后端地址
3. 在微信公众平台配置已认证的小程序，并在 `backend/src/main/resources/application.yml` 的 `wechat.mini-program` 节点填写 AppID 和 AppSecret
4. 用户点击手机号快捷登录并授权后，后端按手机号登录或创建宾客账号
5. 编译运行

现有数据库执行一次迁移脚本：`backend/sql/migrations/V2__wechat_phone_login.sql`。新建数据库则可直接执行 `backend/sql/homestay_db.sql`。

## 测试账号

### 管理后台
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 经营者 | admin | 123456 |
| 经营者 | owner | 123456 |
| 前台 | reception1 | 123456 |
| 房务 | housekeeper1 | 123456 |

### 宾客端
用户点击小程序中的手机号快捷登录并授权后即可登录；员工用户名密码登录保持不变。

## API接口说明

### 认证接口
- `POST /api/auth/employee/login` - 员工登录
- `POST /api/auth/wechat/phone-login` - 微信手机号登录或首次创建宾客账号

### 房间管理
- `GET /api/roomType/available` - 获取可用房型
- `GET /api/roomType/search` - 搜索可用房型
- `GET /api/room/list` - 获取所有房间

### 订单管理
- `POST /api/order` - 创建订单
- `GET /api/order/my` - 获取当前宾客的订单
- `GET /api/order/{id}` - 获取订单详情
- `PUT /api/order/{id}/checkin` - 办理入住
- `PUT /api/order/{id}/checkout` - 办理退房
- `PUT /api/order/{id}/cancel` - 取消订单

### 文件上传
- `POST /api/file/upload` - 上传文件

## 数据库表结构

| 表名 | 说明 |
|------|------|
| sys_employee | 员工表 |
| sys_guest | 宾客表 |
| sys_config | 系统配置表 |
| sys_operation_log | 操作日志表 |
| room_type | 房型表 |
| room | 房间表 |
| booking_order | 订单表 |
| payment_record | 支付记录表 |
| inventory_item | 库存物品表 |
| inventory_record | 库存变动记录表 |
| pos_record | POS消费记录表 |
| cleaning_task | 清洁任务表 |

## 注意事项

1. **密码存储**: 按要求使用明文存储，生产环境请改用加密存储
2. **文件上传**: 上传的文件存储在 `backend/uploads` 目录
3. **跨域配置**: 已配置允许所有来源，生产环境请限制
4. **JWT密钥**: 请修改 `application.yml` 中的 `jwt.secret`

## License

MIT License


### 房型图片 OSS 与初始素材导入

在 `backend/src/main/resources/application.yml` 的 `aliyun.oss` 中填写上海地域 Endpoint、Bucket 和轮换后的 AccessKey。项目使用私有 Bucket，后端生成临时访问链接。Docker 构建时该文件会打入镜像；修改后需重新构建镜像。请勿将真实密钥提交到代码仓库或分享镜像。

准备数据库连接等既有启动配置后，在 `backend` 目录执行下列命令预览六个房型及图片数量，不会写入 OSS 或数据库：

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --room-image-import=E:/qianxun/素材/lr"
```

确认预览中的房型及旧图片后，在同一参数后追加 `--room-image-import-execute` 执行上传和替换。每个房型所有文件上传成功后才更新数据库；失败后可重新运行，但已经上传的对象不会自动删除。备份数据库中的原图片引用后再运行。

小程序需要能通过 HTTPS 访问返回的图片地址；在微信开发者平台配置相应域名，并验证私有 OSS 签名图片能正常显示。
