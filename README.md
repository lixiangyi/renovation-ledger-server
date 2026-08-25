# 装修记账 · 服务端

[装修记账](https://github.com/lixiangyi/renovation-ledger) 的云端 API：账号、账本快照同步、邀请协作。客户端为：

| 端 | 仓库 |
|---|---|
| Android | [renovation-ledger](https://github.com/lixiangyi/renovation-ledger) |
| 微信小程序 | [renovation-ledger-miniprogram](https://github.com/lixiangyi/renovation-ledger-miniprogram) |

客户端**未登录也能本地记账**；登录后通过本服务把账本同步到云端，并按成员权限决定谁能看到哪本账。

界面截图见 [Android README · 界面预览](https://github.com/lixiangyi/renovation-ledger#界面预览)。

---

## 提供什么

**账号**

- 微信登录（App / 小程序不同 `client`）
- 短信验证码登录；新用户默认昵称 `momo-` + 手机号后四位
- 绑定手机、查询 / 修改资料（`GET/PATCH /me`）
- 开发用 `POST /auth/dev-login`（生产务必关闭）

**账本**

- 列表、创建、整本导入、拉取快照、改名
- 按 revision 同步单条预算项（增改删）
- 拥有者软删 / 恢复；协助者离开；转让拥有者

**协作**

- 拥有者生成邀请码；被邀请人可 **preview** 再 **join**
- 成员列表（拥有者 / 协助者）
- 踢出成员

**健康检查**

- `GET /health`

鉴权：登录接口返回 JWT，后续请求带 `Authorization: Bearer <token>`。列表接口只返回**当前用户作为成员**的账本。

---

## 技术栈

- Kotlin · Spring Boot 3.4 · Spring Security · JPA
- PostgreSQL（默认）；测试可用 H2
- JWT（jjwt）

---

## 本地运行

**环境：** JDK 17、本机 PostgreSQL（库名 / 账号默认见 `src/main/resources/application.yml`）

```bash
git clone https://github.com/lixiangyi/renovation-ledger-server.git
cd renovation-ledger-server
# 创建数据库 renovation_ledger，用户 ledger / ledger（或改 yml）
./gradlew bootRun
```

默认端口 **8080**。请把 `app.jwt-secret` 换成足够长的随机串，并配置：

- `WECHAT_MP_APP_ID` / `WECHAT_MP_SECRET`（小程序）
- `WECHAT_APP_APP_ID` / `WECHAT_APP_SECRET`（Android 开放平台）
- 短信网关相关项（开发环境可用 `app.dev-sms-code`，勿用于生产）

`./gradlew test` 跑单元 / 接口测试。

---

## 主要路径

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/wechat` | 微信登录 |
| POST | `/auth/sms/send` · `/auth/sms/login` | 短信登录 |
| GET/PATCH | `/me` | 当前用户 |
| GET/POST | `/ledgers` | 列表 / 创建 |
| GET/PATCH | `/ledgers/{id}` | 快照 / 改名 |
| PUT/DELETE | `/ledgers/{id}/items/{itemId}` | 同步一条预算项 |
| POST | `/ledgers/{id}/invites` | 生成邀请 |
| GET | `/invites/{code}/preview` | 加入前预览 |
| POST | `/invites/join` | 加入账本 |
| GET | `/ledgers/{id}/members` | 成员 |
| POST | `/ledgers/{id}/leave` | 协助者退出 |
| DELETE | `/ledgers/{id}` | 拥有者软删 |

请求体字段以代码中的 DTO 为准（`LedgerDtos.kt`、`AuthService` 等）。

---

## 与客户端的关系

- Android / 小程序各自保留本地账本，打开时 `list + pull` 对齐云端
- 邀请码在个人中心生成；对方看到拥有者昵称和账本名后再确认加入
- 服务端不负责总览指标计算，指标在客户端按同一套口径汇总

---

## License

尚未指定开源协议；默认仅作个人 / 协作项目使用。
