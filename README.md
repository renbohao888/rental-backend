# 短期房屋租赁管理系统（全栈开发）· 后端

基于 Spring Boot 3 + MyBatis-Plus + MySQL + Redis 的房屋租赁管理系统后端 API，提供**租客 / 房东 / 管理员**三端接口，内置 **Redis 缓存热门房源**、**JWT + 拦截器登录校验与角色鉴权**、支付宝沙箱支付、AI 房源推荐助手。前端仓库：[rental-frontend](https://github.com/renbohao888/rental-frontend)。

## 技术栈

- Spring Boot 3.5（Java 21）+ Maven
- MyBatis-Plus 3.5
- MySQL 8 + Redis
- JWT 登录认证
- 支付宝沙箱 SDK

## 功能概览

- **租客端**：注册登录（账号/手机号）、房源浏览搜索、收藏、预约、下单支付、报修、评价、纠纷、好友聊天
- **房东端**：房源发布管理、房态日历、订单、账单、评价回复、维修处理、营销
- **管理后台**：用户管理、房源审核、订单、公告、轮播图、纠纷仲裁、数据统计
- **通用**：AI 租赁助手（意图解析 + 房源推荐）、深色主题、滚动动画

## 核心实现（与简历对应）

- **数据库设计**：`sql/init.sql` 共 **15 张表**（用户 / 房源 / 订单 / 预约 / 报修 / 评价 / 纠纷 / 账单 / 轮播图 / 公告 / 聊天 / 系统配置等），订单表采用**逻辑删除 + 房源标题/封面/押金快照**，`uk_order_no` 保证订单号唯一。
- **三端接口**：租客端（房源检索、下单支付、报修、收藏、评价）、房东端（房源发布管理、订单处理、账单）、管理后台（用户/房源审核、订单、数据统计），统一 `Result` 返回结构 + 全局异常处理。
- **Redis 缓存热门房源**：`@Cacheable(value = "hotRooms")` + `RedisCacheManager`（hotRooms TTL 3 分钟、全局默认 5 分钟、Jackson JSON 序列化），缓解热门房源查询对 MySQL 的压力。
- **JWT + 拦截器**：`WebConfigJwtInterceptor` 拦截 `/api/**` 校验 `Authorization`，解析出 `userId` / `role` 挂到 request；配合自定义注解 `@RequiresRoles` 做角色鉴权（401 / 403 统一 JSON），Redis 存 token 黑名单支持登出。
- **并发下单防重复预订**：以房源为粒度加 Redis 分布式锁（`SETNX + 过期 + value 校验释放`），在锁的临界区内通过独立 Bean `RoomOrderTxService#createOrderInTx`（`@Transactional`）完成「时间重叠校验 + 订单落库」，**保证事务提交后才释放锁**。

## 本地运行

1. **准备数据库**：安装 MySQL 8 并创建数据库
   ```sh
   mysql -u root -p < sql/init.sql
   ```
   `sql/init.sql` 包含全部 15 张表的建表语句。

2. **准备配置**：复制配置模板并填入真实信息
   ```sh
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
   修改 `application.properties` 中的数据库密码、支付宝密钥等。
   > 真实 `application.properties` 已在 `.gitignore` 中，**不会提交到 Git**。

3. **启动 Redis**（默认 `localhost:6379`，无密码）。

4. **运行**：
   ```sh
   mvnw spring-boot:run
   ```
   默认端口 `8080`，健康检查：`http://localhost:8080/api/...`

## 云端部署（环境变量）

> 💡 **不需要支付宝也能完整上线**：本项目内置**模拟支付**（`POST /api/pay/simulate`），
> 前端订单页面点「模拟支付」即可把订单置为已支付。支付宝密钥（`ALIPAY_*`）可全部留空，
> 仅真实支付宝收款才需要配置。短信验证码为测试模式（验证码打印在后端日志），不依赖短信服务商。

| 环境变量 | 说明 | 示例 |
|---|---|---|
| `DB_URL` | MySQL JDBC 地址 | `jdbc:mysql://host:3306/room_rent_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号密码 | `root` / `********` |
| `REDIS_URL` | Redis 完整连接串（含密码） | `redis://default:password@host:6379` |
| `REDIS_PASSWORD` | 可选的 Redis 密码（若 URL 中已含可留空） | 空 |
| `ALIPAY_APP_ID` / `ALIPAY_PRIVATE_KEY` / `ALIPAY_PUBLIC_KEY` | 支付宝密钥 | 沙箱或正式 |
| `ALIPAY_DOMAIN` | 支付回调域名 | `https://your-domain.com` |
| `FILE_UPLOAD_PATH` | 图片上传目录 | `/app/uploads` |

## Docker 部署

项目根目录已提供 `Dockerfile`：

```sh
docker build -t rental-backend .
docker run -d -p 8080:8080 --name rental-backend \
  -e DB_URL='jdbc:mysql://mysql-host:3306/room_rent_db?...' \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_password \
  -e REDIS_HOST=redis-host \
  -e FILE_UPLOAD_PATH=/app/uploads \
  -v rental-uploads:/app/uploads \
  rental-backend
```

## 接口说明

- 登录/注册：`POST /api/user/login`、`POST /api/user/register`、`POST /api/user/smsLogin`
- 房源：`GET /api/room/list`、`GET /api/room/detail/{id}`
- AI 助手：`POST /api/ai/chat`
- 除标注外的接口均需在 Header 携带 `Authorization: <token>`
