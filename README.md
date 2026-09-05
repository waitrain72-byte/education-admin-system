# 教务管理系统

<div align="center">

![Vue](https://img.shields.io/badge/Vue-3.4-42B883?logo=vuedotjs&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-5.7%20%7C%208.x-4479A1?logo=mysql&logoColor=white)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.x-409EFF?logo=element&logoColor=white)
![uni-app](https://img.shields.io/badge/uni--app-小程序端-2B9939)
![GitHub stars](https://img.shields.io/github/stars/waitrain72-byte/education-admin-system?style=social)

</div>

前后端分离的教务管理系统：Web 端基于 Vue 3 + Vite + TypeScript + Element Plus，后端基于 Spring Boot + MyBatis + MySQL，另提供基于 uni-app（Vue 3）的微信小程序端（连接同一后端）。系统包含管理员、教师、学生三类角色，覆盖学院、专业、班级、课程、选课、课表、成绩、考试安排、教室安排、请假、作业、考勤、通知、评教等教务管理功能。

## 我该怎么用这个项目？（先看我）

根据你的目的选择路径，每一节都是手把手步骤，照着做即可：

| 你的目的 | 看哪一节 | 难度 |
|---|---|---|
| 只想在自己电脑上把它跑起来看看效果 | [Web 端部署 → 方式一：Docker 一键部署](#部署说明--web-端) | ⭐ 最简单，只需装一个 Docker |
| 想在代码上二次开发、改功能 | [Web 端部署 → 方式二：手动部署](#方式二手动部署适合二次开发) | ⭐⭐ 需要装 Node/JDK/Maven/MySQL |
| 想要手机端（微信小程序） | [App 端部署（微信小程序）](#部署说明--app-端微信小程序) | ⭐⭐ 需要微信开发者工具 |

## 仓库分支说明

| 分支 | 内容 |
|---|---|
| `cn-en` | **Web 双语版**（最新版本：中英文切换 + 全部功能，本 README 对应此分支） |
| `mobile` | 微信小程序版（uni-app，与 Web 端共用同一后端，功能对齐） |

## 技术栈

- **Web 前端**：Vue 3 + Vite 5 + TypeScript + Element Plus + Pinia + Vue Router + Vue I18n + ECharts + Axios + Vitest + ESLint/Prettier
- **后端**：Java 8 + Spring Boot 2.7.18 + MyBatis + PageHelper + MySQL + JWT + Knife4j + EasyExcel + Spring AOP + Hutool + Easy Captcha
- **小程序端**：uni-app（Vue 3）+ Pinia + 自研轻量 i18n + 纯 CSS 统计图 + WebSocket 实时通知
- **部署**：Docker / docker-compose + nginx + GitHub Actions（CI）

## 功能总览

**三类角色的业务闭环**

- 管理员：学院 / 专业 / 班级 / 教师 / 学生 / 课程 / 通知等基础数据维护，请假审核，日志与权限管理
- 教师：查看自己的课程与学生，成绩录入、考勤登记、作业批改，接收评教、学业预警跟踪与实时推送提醒（数据按教师隔离）
- 学生：选课退课、我的课表、成绩/考勤/作业、请假申请、网上评教、个性化课程推荐

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
| 智能算法 | 课程推荐：基于物品的协同过滤（选课矩阵 + 余弦相似度），冷启动自动降级热门推荐；学业预警：不及格占比/平均分差距/异常考勤率加权合成风险指数，三级预警并经 WebSocket 实时推送提醒 |
| WebSocket 实时通知 | 请假审核、成绩发布、作业批改实时推送学生；提交通知教师；发通知全员广播，断线自动重连 |
| Excel 导入导出 | 学生批量导入（EasyExcel 校验、跳过重复）/ 导出，成绩导出 |
| 防重复提交与 XSS | `@NoRepeatSubmit` 关键写接口 2 秒防重；Jackson 反序列化中和脚本标签 |
| 文件上传安全 | 扩展名白名单（图片/常见文档/压缩包）+ 20MB 大小上限，拒绝可执行等危险文件；Excel 批量导入限 .xlsx/.xls |
| 通用 CRUD 框架 | `useCrud` + `CrudTable`/`CrudPage` + 后端 `CrudController/CrudService/CrudMapper`，18 个管理页样板代码收敛；批量删除收敛为单条 IN 语句（Service 层拦截空集合） |
| 单元测试与规范 | Vitest 16 个用例（Pinia store / useCrud），ESLint + Prettier |
| Docker 化部署 | 三容器编排（MySQL 自动导库 + healthcheck、后端、前端 nginx 反代 `/api`） |

**内置账号**（初始密码均为 `123456`，数据库存 BCrypt 哈希，登录后可修改）：

```text
管理员：admin
教师：luys（路易斯）
学生：zhangsan（张三）、lisi（李四）、wangwu（王五）
```

数据库种子文件 `sql/xm_educational_manager-full.sql`（含全部表结构、索引、RBAC 权限表与授权、精简演示数据：保留账号的课程/选课/成绩/考勤/作业/评教齐全，日志表仅结构），导入即得可演示环境；头像文件仅 5 个，与演示账号一一对应。数据库 ER 图（Mermaid，含设计要点说明）见 [docs/数据库ER图.md](docs/数据库ER图.md)。

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
+-- sql/                          # 全量数据库种子（表结构 + 索引 + 演示数据 + RBAC 授权）
+-- docs/                         # 数据库 ER 图（Mermaid，GitHub 原生渲染）
+-- vue/                          # Web 前端（components/composables/locales/stores/views 等）
+-- springboot/                   # 后端（controller/service/mapper/entity/common 等）
+-- docker-compose.yml            # 三容器编排
+-- files/                        # 文件上传目录（仅 5 个演示头像）
+-- .github/workflows/ci.yml      # CI（本地保留，未随仓库分发）
```

小程序端（`BISHE-mobile`，独立目录 / `mobile` 分支）结构与构建方式见下文 App 端部署。

## 部署说明 · Web 端

### 方式一：Docker 一键部署（推荐新手，全程只需 3 条命令）

这条路不需要你安装 Node.js、JDK、MySQL——它们全部在 Docker 容器里自动搭好。

#### 第 1 步：安装 Docker

Docker 可以理解为"一键把整个运行环境打包带走"的工具。

- **Windows 10/11**：到 [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) 下载 **Docker Desktop**，双击安装（一路默认即可），安装完启动它，等左下角状态变成绿色 **Running**；
  - 如果启动报错提示 WSL2，按提示安装 WSL2 即可（或到"启用或关闭 Windows 功能"里勾选"适用于 Linux 的 Windows 子系统"后重启）；
- **macOS**：同一个页面下载 Mac 版安装即可；
- **Linux 服务器**（Ubuntu/CentOS/Debian）执行：

```bash
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker
```

装好后打开命令行（Windows 按 `Win+R` 输入 `cmd` 回车），输入下面两条命令，**能显示版本号就说明装好了**：

```bash
docker -v
docker compose version
```

#### 第 2 步：下载代码（克隆仓库）

如果没装过 Git：到 [git-scm.com](https://git-scm.com/downloads) 下载安装（一路下一步）。装好后，**选一个你方便找的文件夹**（比如 `D:\`），在它的地址栏输入 `cmd` 回车，执行：

```bash
git clone -b cn-en https://github.com/waitrain72-byte/education-admin-system.git
cd education-admin-system
```

执行完你会发现多了一个 `education-admin-system` 文件夹，代码就在里面。

#### 第 3 步：一条命令启动

在 `education-admin-system` 文件夹里继续执行：

```bash
docker compose up -d --build
```

- **首次运行约 5~10 分钟**，会滚动大量英文日志——这是在下载依赖和构建镜像，属于正常现象，耐心等它结束；
- 之后再启动就是秒级；
- 结束后执行 `docker compose ps`，看到 mysql / backend / frontend 三行状态都是 `Up` 就说明成功了（backend 短暂 Restart 后自愈属正常，它在等数据库就绪）。

这一条命令自动完成了：创建数据库 → 导入全部表结构和演示数据 → 启动后端 → 启动前端。**你不需要手动建库导库。**

#### 第 4 步：打开系统

用浏览器（推荐 Chrome/Edge）打开：

| 地址 | 说明 |
|---|---|
| `http://localhost:8080` | **系统入口**（部署在服务器上则用 `http://服务器IP:8080`） |
| `http://localhost:9091/doc.html` | 接口文档（可选看） |

登录页输入账号密码和图片验证码（**验证码不区分大小写**），选好角色点登录：

```text
管理员：admin      密码 123456
教师：luys         密码 123456
学生：zhangsan     密码 123456（lisi、wangwu 同）
```

三个账号各登录一次，体验不同角色的菜单与数据。**正式使用前请到右上角头像 → 修改密码改掉默认密码。**

#### 第 5 步：日常停止 / 再启动

```bash
docker compose ps                  # 查看三个容器是否在运行
docker compose logs -f backend     # 出问题时看后端日志（按 Ctrl+C 退出查看）
docker compose restart backend     # 重启某个服务（backend/mysql/frontend）
docker compose stop                # 停止（数据保留，下次 start 即可）
docker compose down                # 停止并删除容器（数据卷仍保留，不丢数据）
docker compose down -v             # ⚠️ 连数据一起清空（想彻底重来才用，之后重新 up 会重新导库）
docker compose up -d --build       # 改了代码后重新构建启动
```

#### 环境变量（不改文件改配置）

| 变量 | 作用 | 默认值 |
|---|---|---|
| `JWT_SECRET` | 登录令牌签名密钥 | `change-me-in-production`，**公网部署必须改** |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `123456` |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的前端来源 | `http://localhost:8080` |

用法示例（Windows PowerShell）：

```powershell
$env:JWT_SECRET="一串只有你知道的随机长字符串"; docker compose up -d --build
```

**部署到云服务器给别人访问时**：① 编辑 `docker-compose.yml` 把 mysql 的 `"3306:3306"` 端口映射删掉或改成 `"127.0.0.1:3306:3306"`（数据库绝不暴露公网）；② 改 `JWT_SECRET` 和数据库密码；③ 登录后改掉管理员默认密码；④ 云安全组只需放行 **8080**；⑤ 要 HTTPS 就在前面加一层 nginx/Caddy 反代。

### 方式二：手动部署（适合二次开发）

这条路要真实安装开发环境，好处是改代码能即时看到效果。

#### 1. 安装基础环境（四样）

| 工具 | 下载地址 | 验证命令 |
|---|---|---|
| Node.js 18+ | [nodejs.org](https://nodejs.org/) 选 LTS 版 | `node -v`、`npm -v` |
| JDK 8 | 搜索 "JDK 8 下载"（Oracle 或 Adoptium） | `java -version` |
| Maven 3.6+ | [maven.apache.org](https://maven.apache.org/download.cgi)（解压后把 bin 目录加入 PATH） | `mvn -v` |
| MySQL 5.7 / 8.x | [dev.mysql.com/downloads](https://dev.mysql.com/downloads/)（安装时记住你设置的 root 密码） | `mysql --version` |

每装一个就开一个新命令行窗口执行验证命令，**都报出版本号再继续**。如果提示"不是内部或外部命令"，说明该工具的 bin 目录没加入系统 PATH 环境变量——把安装目录下的 bin 路径追加到"系统属性 → 环境变量 → Path"后重开命令行。

#### 2. 创建数据库并导入演示数据

打开命令行，进入 MySQL 安装目录的 bin 文件夹（或已配置 PATH 则直接执行）：

```bash
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS xm_educational_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -uroot -p123456 xm_educational_manager < sql/xm_educational_manager-full.sql
```

- 第一条：创建数据库（`-p123456` 换成你自己的 root 密码）；
- 第二条：导入全量种子（表结构 + 索引 + 演示数据 + RBAC 授权一次到位），没有任何输出就是成功；
- 如果你的 MySQL 账号密码与后端默认（`root/123456@localhost:3306`）不同，改 `springboot/src/main/resources/application.yml`。

#### 3. 启动后端

```bash
cd springboot
mvn spring-boot:run
```

看到日志出现 `Started SpringbootApplication` 即成功。浏览器打开 `http://localhost:9091/`，返回一段 JSON 就说明后端正常。接口可视化文档在 `http://localhost:9091/doc.html`。

也可以用 IntelliJ IDEA 打开 `springboot` 文件夹，等 Maven 依赖下载完，直接运行 `SpringbootApplication` 类。

#### 4. 启动前端

**新开一个命令行窗口**（后端那个别关）：

```bash
cd vue
npm install        # 首次执行，下载依赖约 2~5 分钟
npm run dev
```

看到 `Local: http://localhost:8080/` 后浏览器打开即可登录（账号同上）。

> 如果 8080 被占用，Vite 会自动改用 8081——而后端跨域白名单只放行 8080，会出现请求失败。结束旧的 node 进程后重启 `npm run dev` 即可（见常见问题 5）。

#### 5. 改代码怎么看效果

前端改 `.vue`/`.ts` 文件保存后浏览器自动热更新；后端改 Java 文件需重启 `mvn spring-boot:run`。改完前端执行 `npm run lint`（格式检查）和 `npm run test`（16 个单元测试）保持代码规范。

### Web 端打包发布

```bash
cd vue && npm run build            # 前端产物在 vue/dist/（生产环境接口走 /api，由 nginx 反代到 9091）
cd springboot && mvn clean package # 后端 jar 在 springboot/target/
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

CI（`.github/workflows/ci.yml`，本地保留未随仓库分发）：前端 lint + test + build，后端 JDK 8 `mvn compile`。需要启用时把 `.github/` 目录推回仓库即可。

## 部署说明 · App 端（微信小程序）

小程序与 Web 端功能对齐（23 个页面、三角色、中英文、深浅色主题与偏好同步），基于 uni-app（Vue 3）连接同一 Spring Boot 后端，并支持 WebSocket 实时通知（成绩发布/作业批改/请假审核/教务通知推送 + 「首页」未读角标）与跨端资料同步（头像等修改后进入相关页面自动拉取，无需重新登录）。交互按移动端习惯适配：底部 TabBar（首页 / 我的）、卡片列表 + 底部弹层表单、触底加载、请假状态筛选、退出登录；管理页面放在分包里按需加载。

### 第 1 步：准备两样东西

1. **微信开发者工具**：到 [developers.weixin.qq.com/miniprogram/dev/devtools/download.html](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html) 下载 **稳定版**，安装；
2. **小程序 AppID**：到 [mp.weixin.qq.com](https://mp.weixin.qq.com) → 立即注册 → 选"小程序" → 用一个没注册过公众平台的邮箱完成注册 → 登录后左侧"开发管理 → 开发设置"里能看到 AppID（形如 `wx1234567890abcdef`）。个人主体免费，够学习演示用。
   - 只想先跑起来、暂时不想注册：微信开发者工具导入项目时可选"测试号"，但无法真机预览。

### 第 2 步：获取小程序代码

```bash
git clone -b mobile https://github.com/waitrain72-byte/education-admin-system.git BISHE-mobile
cd BISHE-mobile
npm install
```

### 第 3 步：改两处配置（必改）

| 文件 | 改什么 |
|---|---|
| `src/manifest.json` | 找到 `"mp-weixin"` 里的 `"appid"`，把值换成你自己的 AppID |
| `src/utils/config.js` | 第 4 行 `baseUrl`：**开发者工具模拟器**用 `http://localhost:9091`；**真机预览**用电脑局域网 IP（命令行执行 `ipconfig`，找"IPv4 地址"，如 `http://192.168.1.10:9091`） |

### 第 4 步：构建并导入开发者工具

```bash
npm run dev:mp-weixin     # 开发模式（改代码实时热更新），产物在 dist/dev/mp-weixin
# 或
npm run build:mp-weixin   # 生产构建，产物在 dist/build/mp-weixin
```

打开微信开发者工具 → **导入项目** → 目录选择 `dist/dev/mp-weixin`（或 `dist/build/mp-weixin`，**注意选的是编译产物目录，不是源码根目录**）→ AppID 填你自己的 → 后端服务选"不使用云服务"→ 导入。

模拟器里立即能看到登录页。**后端要先启动**（Web 端部署的第 3/4 步任选一种方式，保持 9091 在运行）。

### 第 5 步：真机预览（手机上看）

1. 工具栏点**"预览"**按钮，生成二维码，用手机微信扫码；
2. 手机和电脑必须连**同一个 Wi-Fi**；
3. `baseUrl` 必须是电脑的局域网 IP（不是 localhost）；
4. Windows 防火墙放行 9091 端口（管理员 PowerShell 执行）：
   ```powershell
   New-NetFirewallRule -DisplayName 'edu-manager-9091' -Direction Inbound -Protocol TCP -LocalPort 9091 -Action Allow
   ```
5. 若手机上一直加载失败，检查开发者工具 **详情 → 本地设置 → 勾选"不校验合法域名"**；
6. 工具报 `ECONNREFUSED 127.0.0.1:xxxx`：设置 → 代理设置 → 选"不使用任何代理"；
7. **改了代码后要重新点「预览」生成新二维码**——预览二维码是生成那一刻代码的快照，之后改的代码不会进入旧二维码（真机调试则关闭重开一次）。

> 权限说明：Web 端【权限设置】页调整角色授权后，App 端用户**重新登录**即同步菜单显隐（登录时拉取 `/permission/my`），无需重新打包小程序。

### 发布上线（仅演示可跳过）

正式发布要求：① 一个**已备案的 HTTPS 域名**部署后端（localhost/局域网 IP 不行）；② 公众平台"开发管理 → 服务器域名"把域名填入 request / uploadFile 合法域名；③ 开发者工具点"上传"→ 公众平台提交审核 → 审核通过后发布。

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
| 上传文件目录 | 后端启动目录 `user.dir` 下的 `files/`（MD5 去重；演示头像随仓库 `files/` 目录分发，Docker 部署以绑定挂载方式提供） | — |

## 安全加固清单（上线前必做）

- [ ] 数据库不用 root/弱口令，通过配置或环境变量注入
- [ ] `JWT_SECRET` 环境变量覆盖默认值
- [ ] `CORS_ALLOWED_ORIGINS` 收紧到实际前端域名
- [ ] 小程序 AppID 替换为自己的
- [ ] 修改默认管理员密码
- [ ] 保持 SQL 注入防御约定：Mapper 一律 `#{}` 预编译（禁用 `${}`）、不开 `allowMultiQueries`、枚举入参白名单校验
- [ ] 高并发场景：调整 `spring.datasource.hikari.maximum-pool-size`，多实例部署需引入 Redis 外置 Session/验证码

## 常见问题

**1. 提示 `'npm' / 'mvn' / 'mysql' 不是内部或外部命令`**
对应工具没装或没加 PATH。重开一个命令行窗口再试（装完必须开新窗口才生效）；仍不行就检查环境变量 Path 里是否包含该工具的 bin 目录。

**2. 前端页面能打开，但接口请求失败**
依次检查：后端是否启动（开 `http://localhost:9091/` 看有没有 JSON）；`vue/.env.development` 的 `VITE_BASE_URL`；浏览器 F12 控制台是否报跨域（跨域 = 前端没跑在 8080，见问题 5）。

**3. 后端启动失败，提示数据库连接失败**
MySQL 没启动、库没建、备份没导入、账号密码不对——按手动部署第 2 步重来一遍。日志页/主题语言接口报错同理（缺表缺字段就重新导入全量备份）。

**4. 忘记密码**
BCrypt 无法反推原密码。用管理员账号在用户管理页"重置密码"为 `123456`，登录后立即修改。管理员自己忘了密码只能直接改数据库。

**5. 前端端口变成 8081 且接口报跨域错误**
8080 被残留的旧 dev 服务占用。`netstat -ano | findstr 8080` 找 PID，`taskkill /PID <PID> /F` 结束，再重启 `npm run dev`。后端 9091 被占用同理。

**6. 登录提示"账号已锁定"**
连续失败 5 次触发保护，等 10 分钟自动解锁或重启后端（锁定状态在内存中）。

**7. Docker Desktop 启动失败 / 提示 WSL 2**
按报错指引安装 WSL2：管理员 PowerShell 执行 `wsl --install` 后重启电脑再启动 Docker Desktop。

**8. `git clone` 速度很慢或失败**
多试几次；或使用镜像加速（把克隆地址中的 `github.com` 换成 `gitclone.com/github.com` 等加速前缀）；网络允许时配代理最快。

**9. 小程序验证码不显示 / 真机连不上后端**
按 App 端部署"第 5 步真机预览"的 6 条逐项检查（baseUrl、同一 Wi-Fi、防火墙、不校验合法域名、代理设置）。

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
