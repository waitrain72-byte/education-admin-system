# 教务管理系统 - 微信小程序版

与 Web 端（`manager-vue3`）功能对齐的微信小程序版本，基于 **uni-app（Vue 3 + Vite）** 开发，连接同一个 Spring Boot 后端（`manager-vue3/springboot`，端口 9091）。

## 技术栈

- uni-app（Vue 3 组合式 API + Vite），编译目标 `mp-weixin`
- Pinia（用户状态，与 Web 端同构）
- 自研轻量 i18n（词条键与 Web 端 vue-i18n 完全一致，中/英双语）
- CSS 自定义属性主题系统（浅色/深色/跟随系统，与 Web 端共用后端偏好）
- **统一加载动画 `xm-loader`**（方案参考 SpinKit / loaders.css 等高星加载器，「追逐圆点」iOS 原生风，请求层计数驱动 + 500ms 最短展示时长 + 状态订阅制：页面跳转动画不丢失、并发不闪烁，主题变量自动适配深浅色）
- **消息中心**（WebSocket 推送本地按用户持久化：未读角标 + 历史回看 + 按推送内容跳转对应功能页）
- **统计图表**（纯 CSS 实现：考勤环形图 conic-gradient + 成绩柱状图，零依赖、小程序 100% 渲染、深浅主题自适应）
- **教室信息与智能排课**（教室主数据：编号/类型/固定占用，共 413 间含运动场馆；排课教室下拉只列该时段空闲教室；教室留空保存时系统按容量就近自动分配并提示，体育课优先运动场馆；同教室同时段冲突强校验，已结课自动释放）
- **课表周视图**（课程块按课程名哈希自动配色、今日列高亮）
- WebSocket 实时通知（`uni.connectSocket`，心跳保活 + 入站静默检测半开连接 + 断线指数退避重连，推送静默入列：角标未读数 + 页面事件，不打断用户）
- rpx 弹性布局 + `@media` 断点：手机 3 列宫格 / 平板 4 列 / 宽屏 6 列（内容区限宽居中），适配不同设备宽度

### 工程化（对齐 unibest 等高星 uni-app 项目）

- **ESLint**（vue3-essential 扁平配置）+ **Prettier**（格式化各司其职，`eslint-config-prettier` 消除冲突）
- **husky + lint-staged**：提交前自动对暂存文件执行 `eslint --fix` + `prettier --write`
- **Vitest 单元测试**：请求层（401/断网/超时/loading 计数/文件地址归一化）、useCrud（分页去重/空参数剔除/防重复提交/删除确认）、i18n（词条/插值/回退）共 32 条用例
- **GitHub Actions CI**：push/PR 自动执行 `lint → test → build:mp-weixin`（见 `.github/workflows/ci.yml`）
- **渐进式 TypeScript**：请求层 / WebSocket / 配置 / 用户 store 已迁移 TS（`tsconfig.json` 已就位，剩余模块可按需跟进）

## 功能对照（与 Web 端一致）

| 模块             | 说明                                                                                                                                                                                                                                       |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 登录/注册/验证码 | 图形验证码 + Session Cookie 手动维护；学生自助注册                                                                                                                                                                                         |
| 首页             | 欢迎、按角色的功能宫格、**今日课程**（学生/教师：时间轴彩色块，实时「进行中/未开始/已结束」状态）、教务通知、考试安排、考勤统计（环形图）、成绩统计（柱状图）；数据本地缓存秒开（按用户隔离）+ 无缓存时骨架屏                              |
| 智能推荐         | 基于物品的协同过滤课程推荐（学生角色）：按选课相似度生成「为你推荐」并附推荐理由，冷启动降级热门课程，点击直达选课页                                                                                                                       |
| 信息公告         | 教务通知 / 考试安排 / **教室信息管理**（编号/类型/固定占用/容纳人数，管理员维护）                                                                                                                                                          |
| 行政管理         | 学院 / 专业 / 班级（仅管理员；含学院→专业级联下拉）                                                                                                                                                                                        |
| 教学管理         | 课程（含学生选课；**排课教室只列该时段空闲教室，留空保存自动分配**；教师可改本人课程的教室/周几/大节/状态，不可增删课程）、我的选课（取消选课/评教）、我的课表（周视图：课程块按课程自动配色、今日列高亮）、我的成绩（教师录入）、网上评教 |
| 教务管理         | 请假申请（学生申请/撤销、管理员审核）、作业提交（含附件上传、教师打分）、考勤信息；日期字段全部使用原生日期选择器                                                                                                                          |
| 用户管理         | 管理员/教师/学生管理（含重置密码、头像上传、级联归属选择）                                                                                                                                                                                 |
| 个人中心         | 个人信息（按角色差异化字段 + 头像上传）、修改密码                                                                                                                                                                                          |
| 主题             | 浅色/深色/跟随系统三档，登录后与后端同步（账号表 `theme` 列）                                                                                                                                                                              |
| 语言             | 中文/English 切换，登录后与后端同步（账号表 `locale` 列）                                                                                                                                                                                  |
| 实时通知         | WebSocket 推送静默入列：「首页」未读角标累计 + 消息中心落库可回看，点击消息直达对应功能页                                                                                                                                                  |
| 加载体验         | 请求层计数式统一加载动画（并发请求蒙层自动叠加/收起），失败按「断网/超时/其他」细分提示；保存类操作防重复提交                                                                                                                              |
| 跨端资料同步     | 头像等资料在任一端修改后，进入「我的」页自动拉取最新信息，无需重新登录                                                                                                                                                                     |

## 运行

```bash
npm install
npm run dev:mp-weixin     # 开发模式，产物在 dist/dev/mp-weixin
npm run build:mp-weixin   # 生产构建，产物在 dist/build/mp-weixin
```

用微信开发者工具"导入项目"选择对应的 `dist/.../mp-weixin` 目录即可（AppID 在 `src/manifest.json` 的 `mp-weixin.appid` 中填写，或使用测试号）。

### 工程化命令

```bash
npm run lint        # ESLint 检查（CI 同款，不写文件）
npm run lint:fix    # ESLint 自动修复
npm run format      # Prettier 格式化 src
npm test            # Vitest 单元测试（单次）
npm run test:watch  # Vitest 监听模式
```

git 提交时 husky 会自动对暂存文件跑 lint-staged（ESLint + Prettier）。

### 连接后端

1. 启动 Web 端的 Spring Boot 后端（端口 9091）。
2. 开发者工具中勾选 **详情 → 本地设置 → 不校验合法域名**（`urlCheck` 已在 manifest 中默认关闭）。
3. 接口地址在 `.env.development` / `.env.production` 的 `VITE_API_BASE_URL`（由 `src/utils/config.ts` 读取，改环境只改 env 文件）：
   - 开发者工具模拟器：`http://localhost:9091`
   - 真机调试：改为电脑的局域网 IP（如 `http://192.168.x.x:9091`），手机与电脑同一 Wi-Fi
   - 正式发布：改为已备案的 https 域名
4. 数据库使用 Web 端仓库的最新种子 `manager-vue3/sql/xm_educational_manager-full.sql`（已含 `theme`/`locale` 列与 RBAC 授权，无需单独迁移脚本）。
5. 真机联调连不上/头像不显示时，按 `src/utils/config.ts` 头部的排错清单逐项检查；**改代码后需重新点「预览」生成新二维码**（旧二维码是当时代码的快照）。

## 目录结构

```text
src/
+-- components/                 # easycom 业务组件（目录名即标签名，自动注册）
|   +-- xm-loader/              # 统一加载动画（追逐圆点，utils/loading.ts 状态订阅驱动）
|   +-- xm-search-card/         # 通用搜索卡（关键字 + 查询/重置 + filter 插槽）
|   +-- xm-action-bar/          # 通用操作区（新增/批量管理/批量删除）
|   +-- xm-form-popup/          # 通用底部弹层表单（保存防重复提交）
|   +-- xm-list-footer/         # 列表底部反馈条（加载中/没有更多/加载更多）
|   +-- xm-empty/               # 通用空态（图标 + 文案 + 重新加载操作）
+-- i18n/index.js               # 轻量 i18n（词条键与 Web 端一致）
+-- locales/                    # zh-CN / en-US 核心包 + pages/ 分组包
+-- stores/
|   +-- user.ts                 # Pinia 用户状态（uni storage 持久化，密码类字段持久化前剔除）
|   +-- message.ts              # 消息中心仓库（推送本地按用户持久化、未读计数、关键词跳转）
+-- composables/
|   +-- useCrud.js              # 通用 CRUD（分页列表/表单/删除确认/失败页码回滚/保存前钩子，与 Web 端语义对齐）
|   +-- useManage.ts            # 批量管理模式（勾选/退出清空）
|   +-- usePermission.js        # RBAC 权限码拉取（菜单按权限过滤，与 Web 端一致）
|   +-- useTheme.js             # 主题三档切换 + 系统深色跟随 + 后端同步 + 原生导航/tabBar 联动
|   +-- useLocale.js            # 语言偏好 + 后端同步
+-- utils/                      # 核心层（渐进式 TS）
|   +-- config.ts               # 后端地址（头部附真机联调排错清单）
|   +-- request.ts              # uni.request 封装（token 头、验证码 Cookie、401 统一处理（并发防抖）、
|                               #   计数式加载动画、断网/超时细分提示、头像地址归一化）
|   +-- websocket.ts            # 实时通知（静默入列落库 + 角标、心跳、入站静默检测、断线重连）
|   +-- loading.ts              # 加载状态订阅中心（计数 + 最短展示时长，xm-loader 挂载即同步）
+-- styles/theme.scss           # CSS 变量主题 + 通用 xm-* 组件类（对齐高星 uni-app 项目设计规范）
+-- pages/                      # 18 个主包页面（login 为入口，未登录自动跳转；含消息中心）
+-- pages-admin/                # 6 个管理页（分包 + 首页 wifi 预载）
tests/                          # Vitest 单元测试（i18n / request / useCrud / useManage）
.github/workflows/ci.yml        # CI：lint + test + build:mp-weixin
```

## 与 Web 端的交互适配说明

功能一致的前提下，交互形式按移动端习惯做了等价适配：

- 表格 → 卡片列表 + 底部弹层表单；分页 → 上拉/点击"加载更多"。
- 批量删除 → "管理"模式勾选后批量删除（管理员角色）。
- ECharts 图表 → 小程序端首页统计使用**纯 CSS 图表**（conic-gradient 环形图 + 柱状图），数据来源接口不变：`/attendance/getPie`、`/score/getLine`；不引入 ECharts 的原因见「已知取舍」。
- 日期控件 → 原生日期选择器 `picker mode="date"`（后端仍按字符串存储，两端行为一致）；附件选择 → `uni.chooseImage` + `uni.uploadFile`。
- 页面加载 → 统一 `xm-loader` 加载动画蒙层（首屏/搜索时出现，触底加载静默由列表底部反馈）。

## 刻意保留中文的部分（与 Web 端一致）

数据库枚举值（考勤状态、请假状态、课程性质、上课时段等）按中文存储，界面仅翻译展示层文案；后端统计按中文值分组，两端行为一致。

## 已知取舍（生产化改造方向）

- **统计图表使用纯 CSS 而非 ECharts**：ECharts 依赖浏览器全局（window/document）与 canvas 环境，npm echarts 6.x 与 UMD 构建在小程序逻辑层均存在模块加载/渲染兼容问题（组件文件丢失、module not defined 等）。纯 CSS 图表零依赖、渲染确定性 100%；如后续必须使用 ECharts，建议引入 lime-echart 并完整回归真机。
- **教室时段占用为应用层校验**：同一「教室 + 星期 + 大节」的唯一性由保存时校验（5010）与教室唯一编号（5011）保证，数据库层未建组合唯一索引——因「已结课释放」语义与唯一索引冲突；极端并发下存在理论双写窗口，如需强一致可引入排课事务或历史课归档表。
- WebSocket 鉴权把 token 拼在 URL 路径（`/ws/notice/{token}`）：会进服务器访问日志，生产化应改为「连接后首条消息鉴权」，需与后端同步调整。
- 教师课程编辑权限：TEACHER 新增了 `course:manage` 授权（业务层在 CourseService 限制教师仅能改本人课程的教室/时间/状态，不可增删）。**老库升级需手动执行授权**：`INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2, 9);`
- 登录态无 refresh token 机制，token 过期统一按 401 踢回登录页。
