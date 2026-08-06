# 教务管理系统

这是一个前后端分离的教务管理系统，前端基于 Vue 3 + Vite + TypeScript + Element Plus，后端基于 Spring Boot + MyBatis + MySQL。系统包含管理员、教师、学生等角色，主要覆盖学院、专业、班级、课程、选课、课表、成绩、考试安排、教室安排、请假、作业、考勤、通知、评教等教务管理功能。

## 技术栈

### 前端

- Vue 3
- Vite 5
- TypeScript
- Vue Router
- Pinia
- Element Plus
- Axios
- ECharts
- ESLint + Prettier（代码规范）
- Vitest（单元测试）

### 后端

- Java 8
- Spring Boot 2.5.9
- MyBatis
- PageHelper
- MySQL
- JWT
- Hutool
- Easy Captcha

### 部署与工程化

- Docker / docker-compose（MySQL + 后端 + 前端一键启动）
- nginx（前端静态资源托管与接口反向代理）
- GitHub Actions（CI：前端 lint + 单测 + 构建，后端编译）

## 项目特色

本项目在基础教务管理功能之外，补充了安全性、文件存储和可维护性方面的优化，适合作为毕业设计中的系统亮点说明。

### 1. 多角色教务业务闭环

系统围绕管理员、教师、学生三类角色组织功能，覆盖教务系统常见业务场景：

- 管理员负责学院、专业、班级、教师、学生、课程、通知等基础数据维护。
- 教师可参与课程、成绩、作业、考勤、评教等教学相关业务。
- 学生可完成选课、查看课表和成绩、提交作业、请假申请、网上评教等操作。

整体功能从基础信息维护到教学过程管理再到结果反馈，形成较完整的教务管理流程。

### 2. JWT 登录鉴权与统一请求封装

前端通过 Axios 请求拦截器自动携带 `token`，后端通过 JWT 拦截器统一校验登录状态。未登录用户访问业务页面时会被路由守卫拦截并跳转到登录页，减少各业务页面重复写鉴权逻辑。

### 3. BCrypt 密码哈希存储

系统已将用户密码由明文存储优化为 BCrypt 哈希存储：

- 新增用户、注册用户、修改密码时都会先进行 BCrypt 哈希处理。
- 登录时通过 BCrypt 校验密码，不需要反解原密码。
- 兼容历史明文密码，旧账号首次登录成功后会自动升级为 BCrypt 哈希。
- 登录成功返回给前端前会清空 `password` 字段，避免密码哈希进入浏览器本地存储。

### 4. 管理员重置密码

管理员可以在管理员、教师、学生管理页面中为用户重置密码。重置后的默认密码为 `123456`，但数据库中保存的仍然是 BCrypt 哈希值，不会回退成明文密码。

作为安全策略，系统限制管理员不能在列表中重置自己的密码（防止误操作锁死当前登录会话）；管理员本人应通过“修改密码”页面修改密码。

### 5. 头像文件 MD5 去重与安全清理

文件上传逻辑已从“时间戳 + 原文件名”优化为“文件内容 MD5 + 扩展名”：

- 同一张图片重复上传时会复用同一个文件，不会在 `files/` 中生成多份重复图片。
- 用户更新头像后，系统会检查旧头像是否仍被管理员、教师或学生引用。
- 只有当旧头像没有任何用户继续使用时，系统才会删除旧文件，避免误删多人共用头像。
- 文件读取和删除增加了基础路径校验，降低路径穿越风险。

### 6. UTF-8 编码约束

项目增加了 `.editorconfig`，并在 Spring Boot 中配置了 UTF-8 响应编码，降低中文注释、页面文案和接口提示出现乱码的概率。

## 工程化优化

在前述业务功能之外，项目持续进行了工程化层面的优化，主要围绕状态管理、代码复用、构建性能与权限体系四个方面。

### 7. Pinia 统一状态管理

- 用户登录态由手写模块级 `ref` + 散落的 `localStorage` 读写，重构为 `Pinia store` 统一管理（`src/stores/user.ts`）。
- 消除 17 处直接 `JSON.parse(localStorage.getItem('xm-user'))` 调用，统一增加异常保护，避免本地存储被污染时整页白屏。
- 401 响应与退出登录时同步清空内存态与本地存储，修复了"页面显示已登录、存储已清空"的状态不一致问题。

### 8. 配置化通用 CRUD 框架

- 抽取 `useCrud` 组合式函数与 `CrudTable` 通用表格组件，统一分页查询、新增、编辑、删除、批量删除与表单校验逻辑。
- 全部 16 个管理页面的分页、增删改查、批量删除逻辑已完成迁移：约 800 行重复样板代码收敛为约 210 行通用实现，业务页面只需提供「列配置 + 接口路径 + 搜索条件 + 表单」。
- 支持自定义操作列（按角色/状态显隐按钮）、自定义单元格插槽（如头像、文件下载）、保存前后钩子（如同步全局用户状态），可覆盖带级联下拉、文件上传等复杂表单的页面。

### 9. 构建体积与性能优化

- Element Plus 改为按需自动引入（`unplugin-vue-components`），图标从全量注册 294 个精简为实际使用的 13 个。
- ECharts 改为按需注册（饼图、折线图及所需组件），图表库体积从约 1,039 KB 降至 334 KB（gzip 114 KB），降幅约 68%。
- 应用主包从约 1,225 KB 降至 9.5 KB，并通过 `manualChunks` 将 vue / element-plus / echarts / axios 拆分为独立缓存 chunk，首页首次加载 JS 总量约减少 30%。
- 修复图表组件重复挂载导致的 resize 监听器累积与实例泄漏，组件卸载时统一移除监听并 `dispose()` 图表。

### 10. RBAC 权限体系

- 路由级权限：每个路由通过 `meta.roles` 声明可访问角色，路由守卫统一校验，无权限访问跳转 `/403` 页。
- 菜单级权限：侧边栏菜单根据当前角色从路由配置动态生成，与路由权限保持单一数据源。
- 按钮级权限：提供 `v-permission` 指令，按角色控制按钮显隐（如重置密码仅管理员可见）。
- 个人中心路由按角色隔离（管理员/教师/学生各自只能访问自己的信息页）。

### 11. JWT 签名密钥独立化

- 签名密钥从"数据库密码"改为独立配置项 `jwt.secret`（支持环境变量 `JWT_SECRET` 覆盖），过期时间可配置（`jwt.expire-hours`）。
- 用户修改密码后已签发的 token 不再全部失效；拦截器先统一验签（含过期校验）再校验账号有效性。

### 12. 后端代码去重与安全加固

- 抽取泛型 `BaseMapper` / `BaseService`，统一管理员、教师、学生三类账号的登录、新增、改密、增删改查、分页与重置密码逻辑，三个 Service 仅保留角色与 Mapper 配置。
- CORS 由通配符 `*` 收紧为可配置的前端来源白名单（`app.cors.allowed-origins`）。
- 数据库脚本为 10 张表的 20 个外键列补充索引，加速按学院/专业/班级/教师/学生/课程维度的关联查询。

### 13. 代码规范与单元测试

- 引入 ESLint（`eslint-plugin-vue` + `typescript-eslint`）与 Prettier，配置 `npm run lint` / `lint:fix` / `format` 脚本，当前 lint 0 问题。
- 引入 Vitest + @vue/test-utils（happy-dom 环境），为 Pinia 用户状态与 `useCrud` 组合式函数编写 15 个单元测试（`npm run test`），覆盖本地存储异常降级、登录态持久化、分页加载、新增/编辑/删除/批量删除、表单校验拦截等场景。

### 14. 容器化与 CI

- 提供前端（Node 构建 + nginx 托管与反代）、后端（Maven 构建 + JRE）Dockerfile 与 `docker-compose.yml`，一键启动 MySQL（自动导入 SQL）+ 后端 + 前端。
- 生产环境接口统一走 `/api` 前缀（nginx 反代到后端），文件上传返回相对 URL，本地开发与容器部署共用同一套逻辑。
- GitHub Actions 工作流（`.github/workflows/ci.yml`）：前端执行 lint + 单测 + 构建，后端执行 Maven 编译。

## 项目结构

```text
manager-vue3
+-- sql/                          # 数据库初始化脚本（建表 + 初始数据 + 索引）
+-- vue/                         # 前端项目
|   +-- public/                  # 静态资源
|   +-- src/
|   |   +-- assets/              # 图片、全局样式
|   |   +-- components/          # 公共组件（CrudTable 等）
|   |   +-- composables/         # 组合式函数（useCrud 等）
|   |   +-- directives/          # 自定义指令（v-permission）
|   |   +-- stores/              # Pinia 状态管理（含单元测试）
|   |   +-- router/              # 路由配置
|   |   +-- utils/               # Axios 请求封装
|   |   +-- views/               # 页面
|   |       +-- manager/         # 后台管理页面
|   +-- .env.development         # 开发环境接口地址
|   +-- .env.production          # 生产环境接口地址（/api，走 nginx 反代）
|   +-- vite.config.ts           # Vite 配置
|   +-- vitest.config.ts         # 单元测试配置
|   +-- .eslintrc.cjs            # ESLint 配置
|   +-- .prettierrc.json         # Prettier 配置
|   +-- Dockerfile               # 前端镜像（Node 构建 + nginx）
|   +-- nginx.conf               # nginx 站点配置（history 回退 + /api 反代）
|   +-- package.json             # 前端依赖和脚本
+-- springboot/                  # 后端项目
|   +-- src/main/java/com/example
|   |   +-- common/              # 通用返回、常量、枚举、拦截器/CORS 配置
|   |   +-- controller/          # 接口控制层
|   |   +-- entity/              # 实体类
|   |   +-- exception/           # 全局异常处理
|   |   +-- mapper/              # MyBatis Mapper 接口（含泛型 BaseMapper）
|   |   +-- service/             # 业务逻辑层（含泛型 BaseService）
|   |   +-- utils/               # 工具类
|   +-- src/main/resources
|   |   +-- mapper/              # MyBatis XML
|   |   +-- application.yml      # 后端配置
|   +-- pom.xml                  # Maven 依赖
|   +-- Dockerfile               # 后端镜像（Maven 构建 + JRE）
+-- .github/workflows/ci.yml     # GitHub Actions 持续集成
+-- docker-compose.yml           # 容器编排（MySQL + 后端 + 前端）
+-- files/                       # 文件上传目录
+-- .editorconfig                # 编辑器编码与格式约束
+-- README.md
```

## 环境要求

请先确认本机已经安装：

- Node.js 18 或更高版本
- npm
- JDK 8
- Maven 3.6 或更高版本
- MySQL 5.7 或 8.x

可用下面命令检查：

```bash
node -v
npm -v
java -version
mvn -v
mysql --version
```

## 数据库准备

后端默认连接本机 MySQL：

```yaml
server:
  port: 9091

spring:
  datasource:
    username: root
    password: 123456
    url: jdbc:mysql://localhost:3306/xm_educational_manager
```

启动前需要先创建数据库：

```sql
CREATE DATABASE xm_educational_manager
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

如果你的 MySQL 用户名、密码、地址或端口不同，请修改：

```text
springboot/src/main/resources/application.yml
```

数据库初始化脚本位于：

```text
sql/xm_educational_manager.sql
```

脚本包含全部建表语句、初始数据以及外键列索引。创建数据库后直接导入即可：

```bash
mysql -uroot -p123456 xm_educational_manager < sql/xm_educational_manager.sql
```

系统内置管理员账号：

```text
账号：admin
密码：123456
```

（数据库中保存的是 BCrypt 哈希，登录后可自行修改密码。）

## 后端启动

进入后端目录：

```bash
cd springboot
```

安装依赖并启动：

```bash
mvn spring-boot:run
```

启动成功后，后端默认运行在：

```text
http://localhost:9091
```

可访问下面地址测试后端是否启动成功：

```text
http://localhost:9091/
```

正常情况下会返回统一响应格式的数据。

## 前端启动

进入前端目录：

```bash
cd vue
```

安装依赖：

```bash
npm install
```

启动开发服务器：

```bash
npm run dev
```

前端默认运行在：

```text
http://localhost:8080
```

开发环境接口地址配置在：

```text
vue/.env.development
```

当前配置为：

```env
VITE_BASE_URL='http://localhost:9091'
```

也就是说，本地开发时需要先启动后端 `9091` 端口，再启动前端 `8080` 端口。

## 常用命令

### 前端

```bash
cd vue
npm install
npm run dev
npm run lint
npm run test
npm run build
npm run preview
```

说明：

- `npm run dev`：启动本地开发服务
- `npm run lint`：ESLint 检查（当前 0 问题）
- `npm run test`：Vitest 单元测试（15 个用例）
- `npm run build`：类型检查并打包生产文件
- `npm run preview`：本地预览打包结果

### 后端

```bash
cd springboot
mvn spring-boot:run
mvn clean package
```

说明：

- `mvn spring-boot:run`：启动后端服务
- `mvn clean package`：打包后端项目

## 主要功能模块

项目当前包含的主要页面和接口模块包括：

- 登录、注册、验证码、修改密码
- 管理员管理
- 教师管理
- 学生管理
- 学院管理
- 专业管理
- 班级管理
- 课程管理
- 选课管理
- 课表管理
- 成绩管理
- 教务通知
- 考试安排
- 教室安排
- 请假申请
- 作业提交
- 考勤信息
- 网上评教
- 文件上传与下载

## 接口和登录说明

前端请求统一封装在：

```text
vue/src/utils/request.ts
```

登录成功后，用户信息由 Pinia store（`src/stores/user.ts`）统一管理，并持久化到浏览器 `localStorage` 的 `xm-user`。请求拦截器会自动从 store 中取出 `token`，并放入请求头：

```text
token: 用户 token
```

路由守卫配置在：

```text
vue/src/router/router-index.ts
```

未登录访问业务页面时，会自动跳转到：

```text
/login
```

系统实现了三层权限控制（RBAC）：

- 路由级：路由通过 `meta.roles` 声明可访问角色，无权限跳转 `/403`；
- 菜单级：侧边栏按当前角色动态生成；
- 按钮级：`v-permission` 指令控制按钮显隐。

后端基础接口在：

```text
springboot/src/main/java/com/example/controller/WebController.java
```

包含：

- `GET /`：健康访问测试
- `GET /captcha`：验证码
- `POST /login`：登录
- `POST /register`：注册
- `PUT /updatePassword`：修改密码

用户管理相关接口还包含管理员重置密码能力：

```text
PUT /admin/resetPassword/{id}
PUT /teacher/resetPassword/{id}
PUT /student/resetPassword/{id}
```

重置后的默认密码为：

```text
123456
```

注意：数据库中不会保存明文 `123456`，后端会通过 BCrypt 生成哈希后再写入数据库。

## 文件上传说明

文件上传接口在：

```text
springboot/src/main/java/com/example/controller/FileController.java
```

接口路径：

```text
POST /files/upload
GET /files/{filename}
DELETE /files/{filename}
```

上传文件默认保存到后端运行目录下的：

```text
files/
```

如果在 IDE 中从 `springboot` 目录启动，文件通常会保存到：

```text
springboot/files/
```

如果从项目根目录或其他目录启动，保存位置会随 `user.dir` 变化。部署时建议统一固定启动目录，避免上传文件分散到不同位置。

当前文件上传已做去重优化：

- 后端会根据文件内容计算 MD5。
- 文件实际保存名格式为 `md5值.扩展名`。
- 如果同一文件已经存在，后端直接返回已有文件地址，不重复写入。
- 用户头像更新后，系统会检查旧头像是否仍被其他管理员、教师或学生引用；没人使用时才删除旧头像文件。

这可以避免用户多次上传同一张头像导致 `files/` 目录不断堆积，也能避免多人共用头像时误删文件。

## 配置说明

### 后端端口

文件：

```text
springboot/src/main/resources/application.yml
```

配置项：

```yaml
server:
  port: 9091
```

### 数据库地址

文件：

```text
springboot/src/main/resources/application.yml
```

配置项：

```yaml
ip: localhost

spring:
  datasource:
    url: jdbc:mysql://${ip}:3306/xm_educational_manager
```

如果数据库不在本机，可以把 `ip` 改为对应服务器地址。

### 前端接口地址

文件：

```text
vue/.env.development
```

配置项：

```env
VITE_BASE_URL='http://localhost:9091'
```

### JWT 密钥与过期时间

文件：

```text
springboot/src/main/resources/application.yml
```

配置项：

```yaml
jwt:
  secret: ${JWT_SECRET:xm-edu-manager-jwt-secret-2026}
  expire-hours: 2
```

生产环境建议通过环境变量 `JWT_SECRET` 覆盖默认密钥。

### CORS 白名单

```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:8080,http://localhost:5173}
```

如需允许其他前端域名访问，通过环境变量 `CORS_ALLOWED_ORIGINS` 配置逗号分隔的地址。

### 文件访问 URL 前缀

```yaml
files:
  url-prefix: /api/files/
```

默认返回相对路径 `/api/files/`，本地开发（Vite 代理）与容器部署（nginx 反代）均可直接访问；如需自定义可修改该配置。

### Vite 开发端口

文件：

```text
vue/vite.config.ts
```

配置项：

```ts
server: {
  port: 8080
}
```

## 新人接手建议

建议按下面顺序熟悉项目：

1. 先启动 MySQL，并确认 `application.yml` 中的数据库账号密码正确。
2. 启动后端，访问 `http://localhost:9091/` 确认接口服务正常。
3. 启动前端，访问 `http://localhost:8080`。
4. 阅读 `vue/src/router/router-index.ts`，了解页面入口和业务模块。
5. 阅读 `vue/src/utils/request.ts`，了解前端接口请求和 token 处理。
6. 从某个简单模块入手，例如 `Notice` 或 `College`，对照阅读前端页面、后端 Controller、Service、Mapper、XML。
7. 再阅读登录、角色和权限相关逻辑。

一个典型业务模块的阅读路径如下：

```text
vue/src/views/manager/College.vue
    -->
springboot/src/main/java/com/example/controller/CollegeController.java
    -->
springboot/src/main/java/com/example/service/CollegeService.java
    -->
springboot/src/main/java/com/example/mapper/CollegeMapper.java
    -->
springboot/src/main/resources/mapper/CollegeMapper.xml
    -->
springboot/src/main/java/com/example/entity/College.java
```

## 常见问题

### 1. 前端页面能打开，但接口请求失败

请检查：

- 后端是否已经启动
- 后端端口是否为 `9091`
- `vue/.env.development` 中的 `VITE_BASE_URL` 是否正确
- 浏览器控制台是否有跨域或网络错误

### 2. 后端启动失败，提示数据库连接失败

请检查：

- MySQL 是否启动
- 数据库 `xm_educational_manager` 是否存在
- `application.yml` 中的用户名和密码是否正确
- MySQL 端口是否为 `3306`

### 3. 登录失败或验证码错误

登录接口会校验验证码，验证码由后端 Session 保存。请确认：

- 前端请求保留了 cookie/session
- 后端服务没有频繁重启
- 浏览器没有阻止本地 cookie

### 4. 忘记密码怎么办

系统中的密码采用 BCrypt 哈希存储，无法从数据库反推出原密码。如果用户忘记密码，可以由管理员在用户管理页面点击“重置密码”，将该账号密码重置为默认密码 `123456`。

用户使用默认密码登录后，建议立即进入“修改密码”页面设置新密码。

### 5. 中文显示乱码

项目已增加 `.editorconfig`，并在后端配置 UTF-8 响应编码。建议在 IDEA 或 VS Code 中将项目编码设置为 UTF-8。

如果在 Windows PowerShell 中查看中文时出现乱码，可先执行：

```powershell
chcp 65001
```

### 6. 文件上传后找不到文件

文件保存目录依赖后端启动时的 `user.dir`。请确认后端是从哪个目录启动的，并在该目录下查找 `files/` 文件夹。

### 7. 重复上传头像会不会占用越来越多空间

当前上传逻辑已按文件内容 MD5 去重。同一张图片重复上传时不会重复保存。用户更换头像后，系统会在确认旧头像无人引用时自动删除旧文件。

## 打包部署

### 前端打包

```bash
cd vue
npm run build
```

打包产物默认生成在：

```text
vue/dist/
```

部署前请确认生产环境接口地址配置正确。当前 `vue/.env.production` 配置为：

```env
VITE_BASE_URL='/api'
```

生产环境所有接口统一走 `/api` 前缀，由 nginx 反向代理到后端 `9091` 端口；前端静态资源由 nginx 托管。若后端部署在其他地址，调整 nginx 反代目标或改回直连地址即可。

### Docker 部署

项目提供一键容器化部署：

```bash
docker compose up -d --build
```

启动后：

```text
前端：http://localhost:8080
后端：http://localhost:9091
```

MySQL 容器首次启动会自动导入 `sql/` 初始化脚本（建表 + 初始数据 + 索引）。可通过环境变量覆盖默认配置：

```text
JWT_SECRET、CORS_ALLOWED_ORIGINS、SPRING_DATASOURCE_PASSWORD
```

上传文件保存在命名卷 `files_data` 中，数据库数据保存在 `mysql_data` 卷中，重建容器不会丢失。

### 持续集成

推送到 GitHub 后，`.github/workflows/ci.yml` 会自动执行：

- 前端：`npm ci` → `npm run lint` → `npm run test` → `npm run build`
- 后端：JDK 8 环境执行 `mvn compile`

```env
VITE_BASE_URL='http://你的后端地址:9091'
```

### 后端打包

```bash
cd springboot
mvn clean package
```

打包完成后，jar 文件通常位于：

```text
springboot/target/
```

可使用下面命令启动：

```bash
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

## 注意事项

- 项目根目录和 `vue/` 目录都存在 `node_modules`，一般只需要在 `vue/` 目录维护前端依赖。
- 数据库初始化脚本位于 `sql/xm_educational_manager.sql`，新建环境时直接导入即可。
- 生产环境请通过环境变量覆盖默认的 `JWT_SECRET` 与 `CORS_ALLOWED_ORIGINS`。
