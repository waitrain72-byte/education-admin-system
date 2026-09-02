# 教务管理系统

<div align="center">

![Vue](https://img.shields.io/badge/Vue-3.4-42B883?logo=vuedotjs&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.5.9-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-5.7%20%7C%208.x-4479A1?logo=mysql&logoColor=white)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.x-409EFF?logo=element&logoColor=white)
![uni-app](https://img.shields.io/badge/uni--app-小程序端-2B9939)
![GitHub stars](https://img.shields.io/github/stars/waitrain72-byte/education-admin-system?style=social)

</div>

前后端分离的教务管理系统：Web 端基于 Vue 3 + Vite + TypeScript + Element Plus，后端基于 Spring Boot + MyBatis + MySQL，另提供基于 uni-app（Vue 3）的微信小程序端（连接同一后端）。系统包含管理员、教师、学生三类角色，覆盖学院、专业、班级、课程、选课、课表、成绩、考试安排、教室安排、请假、作业、考勤、通知、评教等教务管理功能。

## 仓库分支说明

| 分支 | 内容 |
|---|---|
| `cn-en` | **Web 双语版**（最新版本：中英文切换 + 全部功能，本 README 对应此分支） |
| `mobile` | 微信小程序版（uni-app，与 Web 端共用同一后端，功能对齐） |

## 技术栈

- **Web 前端**：Vue 3 + Vite 5 + TypeScript + Element Plus + Pinia + Vue Router + Vue I18n + ECharts + Axios + Vitest + ESLint/Prettier
- **后端**：Java 8 + Spring Boot 2.5.9 + MyBatis + PageHelper + MySQL + JWT + Knife4j + EasyExcel + Spring AOP + Hutool + Easy Captcha
- **小程序端**：uni-app（Vue 3）+ Pinia + 自研轻量 i18n + 纯 CSS 统计图
- **部署**：Docker / docker-compose + nginx + GitHub Actions（CI）

## 功能总览

**三类角色的业务闭环**

- 管理员：学院 / 专业 / 班级 / 教师 / 学生 / 课程 / 通知等基础数据维护，请假审核，日志与权限管理
- 教师：查看自己的课程与学生，成绩录入、考勤登记、作业批改，接收评教（数据按教师隔离）
- 学生：选课退课、我的课表、成绩/考勤/作业、请假申请、网上评教

**系统亮点**

| 方面 | 说明 |
|---|---|
| RBAC 权限（数据库可配置） | `@RequirePermission` 注解 + AOP 切面做接口级鉴权；`sys_role` / `sys_permission` / `sys_role_permission` 三表存授权，【系统管理→权限设置】页在线勾选即时生效；ADMIN 超管放行；路由级（`meta.roles`）、菜单级、按钮级（`v-permission`）三层联动 |
| JWT 登录鉴权 | 独立签名密钥（`JWT_SECRET` 可覆盖）、过期时间可配；Axios 拦截器自动带 token，401 统一处理 |
| 密码安全 | BCrypt 哈希存储（历史明文首次登录自动升级）；管理员可重置密码为 `123456`（库里存哈希），且不能重置自己 |
| 登录保护 | 图形验证码 + 连续失败 5 次锁定 10 分钟（错误码 4008），成功/失败均写登录日志 |
| 操作/登录日志 | AOP 自动记录非 GET 请求（密码脱敏、超长截断），每天凌晨 2 点定时清理 90 天前日志 |
| 数据隔离 | 教师只看自己任课课程的成绩/考勤/作业/评教，学生只看自己的数据，管理员看全部 |
| 中英文国际化 | vue-i18n 全页面覆盖；语言/主题偏好存数据库，一次设置多端同步 |
| 深浅色主题 | light / dark / 跟随系统三档，CSS 变量体系 + Element Plus 暗色联动 + ECharts 重绘 |
| 数据大屏 | `/dashboard` 全屏页：指标卡 + 成绩分布/考勤占比/学院人数/选课热度/职称结构/登录趋势六图 |
| WebSocket 实时通知 | 请假审核、成绩发布、作业批改实时推送学生；提交通知教师；发通知全员广播，断线自动重连 |
| Excel 导入导出 | 学生批量导入（EasyExcel 校验、跳过重复）/ 导出，成绩导出 |
| 防重复提交与 XSS | `@NoRepeatSubmit` 关键写接口 2 秒防重；Jackson 反序列化中和脚本标签 |
| 通用 CRUD 框架 | `useCrud` + `CrudTable`/`CrudPage` + 后端 `CrudController/CrudService/CrudMapper`，18 个管理页样板代码收敛 |
| 单元测试与规范 | Vitest 16 个用例（Pinia store / useCrud），ESLint + Prettier |
| Docker 化部署 | 三容器编排（MySQL 自动导库 + healthcheck、后端、前端 nginx 反代 `/api`） |

**内置账号**（初始密码均为 `123456`，数据库存 BCrypt 哈希，登录后可修改）：

```text
管理员：admin
教师：t01 ~ t20（职称各不相同）
学生：2024001 ~ 2024050（分属 5 个班级）
```

数据库备份为**全量**文件 `sql/xm_educational_manager-full.sql`（含全部表结构、日志表、RBAC 权限表与授权、完整演示数据：每位教师的课程/选课/成绩/考勤/作业/评教都齐全），导入即得完整可演示环境。

## 核心模块之间的联系（数据怎么流转）

系统以 **「课程」** 为中心把各模块串联起来，理解下面这条主线就知道每个模块的数据从哪来、给谁用：

```
学院 → 专业 → 班级 → 学生
                    └─ 学生选课 ─┐
教师 ──► 课程 ──────────────────┤
                              ├─► 我的课表（选课生成的课程表）
                              ├─► 成绩管理（教师给所选学生录成绩）
                              ├─► 考勤信息（教师给所选学生记考勤）
                              ├─► 作业提交（学生提交作业 → 教师批改打分）
                              └─► 网上评教（学生对所选课程评教 → 教师查看）
```

- **基础档案链**：`学院 → 专业 → 班级 → 学生` 层层包含，数据大屏的"各学院学生人数"、学生管理的级联下拉都依赖这条链。
- **课程与教师的归属**：每门课程有唯一任课教师（`course.teacher_id`），教师登录后"课程信息"只显示自己任教的课程。
- **选课是枢纽**：`choice` 表把「学生 ↔ 课程 ↔ 教师」绑定，课表、成绩、考勤、作业、评教全部围绕"某学生选了某老师的某门课"展开。
- **权限隔离口径**：教师/学生按身份看数据（教师看自己任课课程，学生看自己），管理员看全部。
- **数据大屏**：汇总账号表（学生/教师数）、`choice`（选课人次）、`score`（成绩分布）、`attendance`（考勤占比）等。
- **请假申请**：学生提交、管理员审核（结果实时推送），独立于课程链。
- **通知 / 考试安排 / 教室安排**：全员公告性数据，出现在首页与数据大屏。

## 项目结构

```text
manager-vue3
+-- sql/                          # 全量数据库备份（表结构 + 完整演示数据 + RBAC 授权）
+-- sql/rbac_permission.sql       # RBAC 脚本（仅存量旧库补齐用；新备份已内置，勿重复执行）
+-- vue/                          # Web 前端（components/composables/locales/stores/views 等）
+-- springboot/                   # 后端（controller/service/mapper/entity/common 等）
+-- docker-compose.yml            # 三容器编排
+-- files/                        # 文件上传目录（含演示头像）
+-- .github/workflows/ci.yml      # CI（本地保留，未随仓库分发）
```

小程序端（`BISHE-mobile`，独立目录 / mobile 分支）结构与构建方式见下文 App 端部署。

## 部署说明 · Web 端

### 方式一：Docker 一键部署（推荐）

```bash
git clone -b cn-en https://github.com/waitrain72-byte/education-admin-system.git
cd education-admin-system
docker compose up -d --build      # 首次构建约 5~10 分钟，之后秒级
```

启动三个容器：`frontend`（nginx 托管前端，8080）→ `/api` 反代 → `backend`（Spring Boot，9091）→ `mysql`（3306，首次启动自动导入全量备份，含 healthcheck 等待）。数据分别存于 `mysql_data`、`files_data` 卷，重建容器不丢失。

| 地址 | 说明 |
|---|---|
| `http://localhost:8080` | 系统入口（服务器部署用 `http://服务器IP:8080`） |
| `http://localhost:9091/` | 后端健康检查 |
| `http://localhost:9091/doc.html` | Knife4j 接口文档 |

日常管理：

```bash
docker compose ps                  # 查看容器状态
docker compose logs -f backend     # 实时后端日志
docker compose restart backend     # 重启单个服务
docker compose down                # 停止并删除容器（数据卷保留）
docker compose down -v             # ⚠️ 连数据卷一起删（彻底重来才用）
```

**环境变量**（不改文件覆盖配置）：

| 变量 | 作用 | 默认值 |
|---|---|---|
| `JWT_SECRET` | 登录令牌签名密钥 | `change-me-in-production`，**公网部署必须改** |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `123456` |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的前端来源 | `http://localhost:8080` |

**公网服务器部署注意**：数据库端口不要暴露公网（删除 mysql 的 `ports` 或改绑 `127.0.0.1`）；改 `JWT_SECRET` 与数据库密码；改默认管理员密码；安全组只放行 8080；需要 HTTPS 时前面加一层 nginx/Caddy 反代。

### 方式二：手动部署

环境要求：Node.js 18+、JDK 8、Maven 3.6+、MySQL 5.7/8.x。

**1. 数据库**：创建库并导入全量备份（内部已 `CREATE DATABASE IF NOT EXISTS` + `SET NAMES utf8mb4`，中文不乱码）：

```bash
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS xm_educational_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -uroot -p123456 xm_educational_manager < sql/xm_educational_manager-full.sql
```

> `sql/rbac_permission.sql` 仅供导入过旧版备份的存量库单独补 RBAC 表用（幂等）；全新导入 `full.sql` 后**不要再执行**，否则会把自定义授权重置为默认。

数据库连接配置在 `springboot/src/main/resources/application.yml`（默认 `root/123456@localhost:3306`），按需修改。

**2. 后端**：

```bash
cd springboot
mvn spring-boot:run        # 启动后访问 http://localhost:9091/ 验证
```

**3. 前端**：

```bash
cd vue
npm install
npm run dev                # http://localhost:8080（开发代理 /api → 9091）
```

> 注意：若 8080 被占用 Vite 会顺延到 8081，而后端 CORS 白名单只放行 8080，会出现跨域错误——结束残留 node 进程后重启即可。

### 打包与发布

```bash
cd vue && npm run build            # 前端产物 vue/dist/（生产走 /api，由 nginx 反代到 9091）
cd springboot && mvn clean package # 后端 jar 在 springboot/target/
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

CI（`.github/workflows/ci.yml`，本地保留未随仓库分发）：前端 lint + test + build，后端 JDK 8 `mvn compile`。需要启用时把 `.github/` 目录推回仓库即可。

## 部署说明 · App 端（微信小程序）

小程序与 Web 端功能对齐（23 个页面、三角色、中英文、深浅色主题与偏好同步），基于 uni-app（Vue 3）连接同一 Spring Boot 后端。交互按移动端习惯适配：表格 → 卡片列表 + 底部弹层表单、分页 → 触底加载、批量删除 → "管理"模式勾选、ECharts → 纯 CSS 统计条；含底部 TabBar（首页 / 我的）、管理页面分包加载、请假状态快捷筛选、退出登录。

环境要求：Node.js 18+、微信开发者工具（最新稳定版）。

**复用者必改三处**：

| 位置 | 改什么 |
|---|---|
| `src/manifest.json` → `mp-weixin.appid` | 换成自己的小程序 AppID（[mp.weixin.qq.com](https://mp.weixin.qq.com) 注册，个人主体免费） |
| `src/utils/config.js` → `baseUrl` | 后端地址：模拟器用 `http://localhost:9091`，真机用电脑局域网 IP（`ipconfig` 查看） |
| 数据库 | 确认已导入全量备份（Web 端部分已覆盖） |

**构建与导入**：

```bash
cd BISHE-mobile
npm install
npm run dev:mp-weixin     # 开发模式（热更新），产物 dist/dev/mp-weixin
npm run build:mp-weixin   # 生产构建，产物 dist/build/mp-weixin
```

微信开发者工具：**导入项目 → 选择 `dist/build/mp-weixin`（编译产物目录，不是源码根目录）→ 后端服务选"不使用云服务"**。

**真机调试检查清单**：

1. 开发者工具 详情 → 本地设置 → 勾选"不校验合法域名"；
2. 手机与电脑连同一个 Wi-Fi；
3. Windows 防火墙放行 9091：`New-NetFirewallRule -DisplayName 'edu-manager-9091' -Direction Inbound -Protocol TCP -LocalPort 9091 -Action Allow`；
4. `baseUrl` 用电脑当前有效的局域网 IP，改完需重新构建；
5. 工具若报 `ECONNREFUSED 127.0.0.1:xxxx`，在 设置 → 代理设置 改为"不使用任何代理"。

**发布上线**（仅演示可跳过）：需要已备案的 HTTPS 域名部署后端；在公众平台"服务器域名"填入 request / uploadFile 合法域名；开发者工具上传 → 提交审核 → 发布。

> 权限说明：Web 端【权限设置】页调整角色授权后，App 端用户**重新登录**即同步菜单显隐（登录时拉取 `/permission/my`），无需重新打包小程序。

## 配置速查

| 配置 | 文件 | 默认值 |
|---|---|---|
| 后端端口 | `springboot/src/main/resources/application.yml` | `9091` |
| 数据库连接 | 同上（`ip` / `spring.datasource.*`） | `localhost:3306/xm_educational_manager`，`root/123456` |
| JWT 密钥/过期 | 同上 `jwt.*`（支持环境变量 `JWT_SECRET`） | 内置默认值 / `2` 小时 |
| CORS 白名单 | 同上 `app.cors.allowed-origins`（环境变量 `CORS_ALLOWED_ORIGINS`） | `http://localhost:8080,http://localhost:5173` |
| 文件访问前缀 | 同上 `files.url-prefix` | `/api/files/` |
| 前端接口地址 | `vue/.env.development`（开发）/ `.env.production`（生产 `/api`） | 见文件 |
| 前端开发端口 | `vue/vite.config.ts` | `8080` |
| 上传文件目录 | 后端启动目录 `user.dir` 下的 `files/`（MD5 去重，无人引用的旧头像自动清理） | — |

## 安全加固清单（上线前必做）

- [ ] 数据库不用 root/弱口令，通过配置或环境变量注入
- [ ] `JWT_SECRET` 环境变量覆盖默认值
- [ ] `CORS_ALLOWED_ORIGINS` 收紧到实际前端域名
- [ ] 小程序 AppID 替换为自己的
- [ ] 修改默认管理员密码
- [ ] 保持 SQL 注入防御约定：Mapper 一律 `#{}` 预编译（禁用 `${}`）、不开 `allowMultiQueries`、枚举入参白名单校验
- [ ] 高并发场景：调整 `spring.datasource.hikari.maximum-pool-size`，多实例部署需引入 Redis 外置 Session/验证码

## 常见问题

**1. 前端页面能打开，但接口请求失败**
检查后端是否启动、端口是否 9091、`vue/.env.development` 的 `VITE_BASE_URL`、浏览器控制台是否跨域/网络错误。

**2. 后端启动失败，提示数据库连接失败**
检查 MySQL 是否启动、库 `xm_educational_manager` 是否存在（未导入备份则导入 `sql/xm_educational_manager-full.sql`）、`application.yml` 账号密码端口是否正确。

**3. 忘记密码**
BCrypt 无法反推原密码。由管理员在用户管理页"重置密码"为 `123456`，登录后立即修改。管理员忘记自己的密码需直接改库（重置为 BCrypt 哈希）。

**4. 启动后端报 `Port 9091 was already in use`**
旧实例占用：`netstat -ano | findstr 9091` 找 PID，`taskkill /PID <PID> /F` 结束。

**5. 前端端口变成 8081 且接口报跨域错误**
8080 被残留 dev 服务占用，Vite 顺延端口而 CORS 只放行 8080。结束多余 node 进程后重启 `npm run dev`。

**6. 登录提示"账号已锁定"**
连续失败 5 次触发登录保护，等 10 分钟自动解锁或重启后端（锁在内存中）。

**7. 操作/登录日志页面报 500 或主题/语言接口报错**
数据库备份未导入或版本过旧（缺日志表、`theme`/`locale` 字段），重新导入全量备份后重启后端。

**8. 小程序验证码不显示 / 真机连不上后端**
见上文 App 端部署的"真机调试检查清单"（baseUrl、同网段、防火墙、不校验合法域名、代理设置）。

## 维护约定（两端必读）

1. **用户偏好多端同步**：主题（`theme`：light/dark/system）与语言（`locale`：zh-CN/en-US）存账号表，两端登录自动拉取、修改后防抖推送。
2. **语言包词条键两端一致**：Web（vue-i18n）与小程序（自研 i18n）使用相同键名，改文案两端同步。
3. **数据库枚举值为中文**：考勤状态、请假状态、课程性质等按中文入库，后端统计按中文分组——界面只翻译展示文案，不要改入库枚举值。
4. **接口约定**：统一返回 `{ code, msg, data }`；token 放自定义请求头 `token`；错误码见 `ResultCodeEnum`，前端按码本地化、未知码回退后端消息。
5. **日志约定**：操作日志由 AOP 自动记录非 GET 请求（新接口无需埋点）；登录日志由 `LoginProtectService` 记录。
6. **典型模块阅读路径**：`vue/src/views/manager/College.vue` → `controller/CollegeController.java` → `service/CollegeService.java` → `mapper/CollegeMapper.java` → `resources/mapper/CollegeMapper.xml` → `entity/College.java`。

## 可个性化调整

| 想改什么 | 位置 |
|---|---|
| 系统名称 | Web：`vue/src/locales/zh-CN.ts` 的 `layout.title`；小程序：`BISHE-mobile/src/locales/zh-CN.js` 同名键（中英两个语言包都要改） |
| Logo / 配色 | `vue/src/assets/imgs/`；主题变量 Web `vue/src/assets/css/theme.css`、小程序 `BISHE-mobile/src/styles/theme.scss` |
| 大屏样式 | `vue/src/views/Dashboard.vue`（1920×1080 等比缩放） |
| 演示数据 | `sql/` 下备份脚本可按需修改 |
