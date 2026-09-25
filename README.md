# 教务管理系统 - 微信小程序版

与 Web 端（`manager-vue3`）功能对齐的微信小程序版本，基于 **uni-app（Vue 3 + Vite）** 开发，连接同一个 Spring Boot 后端（`manager-vue3/springboot`，端口 9091）。

## 技术栈

- uni-app（Vue 3 组合式 API + Vite），编译目标 `mp-weixin`
- Pinia（用户状态，与 Web 端同构）
- 自研轻量 i18n（与 Web 端共有的词条键完全一致，移动端独有功能另加词条；中/英双语）
- CSS 自定义属性主题系统（浅色/深色/跟随系统，与 Web 端共用后端偏好）；小程序开启 `darkmode` + `src/theme.json`，「跟随系统」冷启动即读取手机深浅色，原生导航栏 / tabBar 与页面主题逐页同步
- **自定义主题色**（8 个预设色板，按账号存后端、与 Web 端双向同步；配色算法与 Web 端逐位一致，深色模式自动提亮；原生导航栏 / tabBar 文字同步变色）
- **请求层契约与 Web 端统一**：`code === '200'` 直接返回业务数据，非 200 统一提示（同文案 2 秒去重）并以 `ApiError` 拒绝，页面只写成功分支
- **统一登录拦截**：`uni.addInterceptor` 拦截四种路由跳转 + 页面 `ensureLoggedIn()` 兜底（覆盖原生 tabBar 点击、冷启动、分享进入等拦截器管不到的入口）
- **全局错误上报**：`onError` / `onUnhandledRejection` / `onPageNotFound` 写入微信「实时日志」，真机报错可在小程序后台直接查到（业务错误与 401 属预期流程不上报，同类错误 10 秒内去重）
- **线性图标 `xm-icon`**（Feather 风格 SVG 经 CSS mask 渲染、颜色取 currentColor：跟随主题色与深浅色，替代各机型显示不一致的 emoji；做法同 unibest 在小程序里用 UnoCSS 图标）
- **统一加载动画 `xm-loader`**（方案参考 SpinKit / loaders.css 等高星加载器，「追逐圆点」iOS 原生风，请求层计数驱动 + 500ms 最短展示时长 + 状态订阅制：页面跳转动画不丢失、并发不闪烁，主题变量自动适配深浅色）
- **消息中心**（WebSocket 推送按账号（角色-id）本地持久化：未读角标 + 历史回看 + 按推送内容跳转对应功能页）
- **统计图表**（纯 CSS 实现：考勤环形图 conic-gradient + 成绩柱状图，零依赖、小程序 100% 渲染、深浅主题自适应）
- **教室信息与智能排课**（教室主数据：编号/类型/固定占用/容纳人数，含运动场馆；排课教室下拉只列该时段空闲教室；教室留空保存时系统按容量就近自动分配并提示，体育课优先运动场馆；同教室同时段冲突强校验，已结课自动释放）
- **课表周视图**（一屏放下整周、周末无课自动收起；课程块按课程名哈希配色，今日列高亮 + 进行中描边 + 当前时间红线；点课程弹出教师/教室/学分详情；本地缓存，弱网 / 断网也能看上次同步的课表）
- **学业预警**（后端按成绩与考勤的多指标加权模型算出 0~100 风险指数；学生看本人、教师看任课学生、管理员看全部，按等级筛选 + 实时推送提醒）
- **成绩汇总**（学生成绩页：平均学分绩点 / 加权平均分 / 已获学分 / 不及格门数，按课程学分加权）
- **本地数据按账号隔离**：首页缓存、课表缓存、消息历史的键都是「角色-id」（管理员 / 教师 / 学生分表存储，id 会重复），同一台手机切换账号互不串数据
- **小程序上线细节**：新版本提示（`getUpdateManager`，新版本下载完成后提示重启）；右上角「转发」（公开信息页转发当前页，其余转发首页）
- WebSocket 实时通知（`uni.connectSocket`，心跳保活 + 入站静默检测半开连接 + 断线指数退避重连，推送静默入列：角标未读数 + 页面事件，不打断用户）
- **多设备宽度适配**（手机 / 折叠屏 / 平板 / PC 窗口）：
  - 尺寸：手机（≤414px）按 rpx 等比缩放；更宽的设备由构建期 PostCSS 插件（`src/utils/rpxCap.js`，在 `vite.config.js` 注册）为每条含 rpx 的规则追加 `@media (min-width: 415px)` 的 px 覆盖，元素停在大屏手机的尺寸、不再随屏宽放大；只用媒体查询 + px，小程序与 H5 均直接生效，写样式照常用 rpx。JS 里拼的尺寸（图标、课表行高）用 `useScreen` 的 `rpx()`，规则一致
  - 布局：<720px 单列；≥720px（平板竖屏 / 折叠屏）列表两列、底部弹层居中限宽 600px、表单 / 设置类页面居中限宽；≥1248px（平板横屏 / PC）列表三列、内容区最宽 1200px 居中；首页宫格 3 / 4 / 6 / 8 列
  - 小程序开启 `resizable`：iPad 可旋转横屏、PC 端窗口可拖动缩放；页面底部、弹层、悬浮按钮避让刘海屏 Home 条（`env(safe-area-inset-bottom)`）
  - 已在 320 / 375 / 414 / 430 / 600 / 768 / 1024 / 1366 八种宽度下截图并测量：无横向溢出，字号 414px 以下等比、以上恒定，列数与弹层宽度符合上述断点

### 工程化（对齐 unibest 等高星 uni-app 项目）

- **ESLint**（vue3-essential 扁平配置）+ **Prettier**（格式化各司其职，`eslint-config-prettier` 消除冲突）
- **husky + lint-staged**：提交前自动对暂存文件执行 `eslint --fix` + `prettier --write`
- **接口层 `src/api`**：业务接口路径全部集中定义，方法名与后端一致（通用的 7 个接口对应后端 `CrudController`），页面只调函数；后端改路径只改一处，新增 CRUD 模块只需一行 `createCrudApi('/xxx')`
- **Vitest 单元测试**（25 个文件共 192 条用例）：
  - 请求层：401/断网/超时/loading 计数/文件地址归一化/`ApiError`/提示去重/非包装响应透传
  - 接口层：7 个标准接口与各模块额外接口的方法 / 路径 / 参数 / 请求选项、按角色取账号接口、验证码会话 Cookie 与取图失败
  - WebSocket：连接幂等、推送入列与角标（99+）、心跳、半开连接检测、指数退避重连与上限、主动关闭不重连
  - 主题色：色值归一化、Element Plus 同款混色算法、深色提亮（与 Web 端同色同值的跨端用例）、原生导航色
  - 上传：token 头、401、业务失败 `ApiError`、非 JSON 响应、进度回调、大小上限与取消、聊天文件扩展名白名单
  - 列表页骨架：useCrud（含返回页面不重载）/ useListPage（搜索条件与重置、角色拦截、附属数据随列表加载、专用删除文案、编辑覆盖字段）/ 重置密码 / 保存后同步当前账号（按角色 + id 判断）
  - 业务枚举表（中英文案齐全、标签色、下拉选项）/ xm-picker 纯函数（下标、展示文案、空值）
  - 附件文件名与查看（图片预览、文档下载后打开、失败复制链接）/ 排课教室简述与保存前自动分配
  - 成绩汇总（绩点换算边界、学分加权、缺学分课程）/ 按账号隔离的本地缓存 / 新版本提示 / 转发规则
  - 登录拦截 / 错误上报 / 消息中心 / 今日课程节次解析与排序 / 周课表网格与时间线 / 首页问候与日期 / 系统深浅色读取 / rpx 大屏封顶插件 / 表单接力聚焦 / i18n
- **发布前地址校验**：构建时检查 `VITE_API_BASE_URL`，非 https / 局域网 IP / 带端口时输出告警；`RELEASE=1` 时直接构建失败，防止把本机地址打进上线包
- **渐进式 TypeScript**：请求层 / WebSocket / 配置 / 用户 store 已迁移 TS（`tsconfig.json` 已就位，剩余模块可按需跟进）

## 功能对照（与 Web 端一致，另有移动端增强）

| 模块             | 说明                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 登录/注册/验证码 | 图形验证码 + Session Cookie 手动维护（验证码一码一用，刷新时自动清空已输入内容）；学生自助注册；未登录访问任何业务页都会被拦截回登录页                                                                                                                                                                                                                                                                                                         |
| 首页             | 按时段问候（早上好 / 下午好…）、按角色的功能宫格、**今日课程**（学生/教师：时间轴彩色块，实时「进行中/未开始/已结束」状态）、教务通知、考试安排、考勤统计（环形图）、成绩统计（柱状图）；数据本地缓存秒开（按账号隔离）+ 无缓存时骨架屏                                                                                                                                                                                                        |
| 智能推荐         | 基于物品的协同过滤课程推荐（学生角色）：按选课相似度生成「为你推荐」并附推荐理由，冷启动降级热门课程，点击直达选课页                                                                                                                                                                                                                                                                                                                           |
| 信息公告         | 教务通知 / 考试安排 / **教室信息管理**（编号/类型/固定占用/容纳人数，管理员维护）                                                                                                                                                                                                                                                                                                                                                              |
| 行政管理         | 学院 / 专业 / 班级（仅管理员；含学院→专业级联下拉）                                                                                                                                                                                                                                                                                                                                                                                            |
| 教学管理         | 课程（含学生选课；**排课教室只列该时段空闲教室，留空保存自动分配**；教师可改本人课程的教室/周几/大节/状态，不可增删课程）、我的选课（取消选课/评教）、我的课表（周视图：课程块按课程自动配色、今日列高亮；**本地缓存**，弱网 / 断网也能看上次同步的课表）、我的成绩（教师录入；学生页顶部显示**平均学分绩点 / 加权平均分 / 已获学分 / 不及格门数**，按课程学分加权；课程筛选按角色取：学生取已选课程、管理员取全部、教师取本人课程）、网上评教 |
| 教务管理         | 请假申请（学生申请/撤销、管理员审核）、作业提交（附件可选相册图片或**微信聊天里的 PDF / Word / Excel 等文档**，按钮显示上传进度、本地拦截超 20MB 与不支持的类型；教师打分）、考勤信息；日期字段全部使用原生日期选择器                                                                                                                                                                                                                          |
| 学业预警         | 与 Web 端同一模型（不及格占比 40% + 平均分差距 30% + 异常考勤率 30%，合成 0~100 风险指数）：学生看本人、教师看本人任课学生、管理员看全部；等级筛选附人数，风险条按等级着色，教师 / 管理员可实时推送提醒，学生点推送消息直达预警页                                                                                                                                                                                                              |
| 用户管理         | 管理员/教师/学生管理（含重置密码、头像上传、级联归属选择）                                                                                                                                                                                                                                                                                                                                                                                     |
| 个人中心         | 个人信息（按角色差异化字段 + 头像上传）、修改密码                                                                                                                                                                                                                                                                                                                                                                                              |
| 主题             | 浅色/深色/跟随系统三档，登录后与后端同步（账号表 `theme` 列）                                                                                                                                                                                                                                                                                                                                                                                  |
| 主题色           | 「我的」页 8 个预设色板点选即生效，按账号存后端（`/themeColor`）与 Web 端双向同步；Web 端选的任意颜色在小程序照常生效并标注「自定义」；退出登录复位，避免下一个账号看到上一个账号的配色                                                                                                                                                                                                                                                        |
| 语言             | 中文/English 切换，登录后与后端同步（账号表 `locale` 列）                                                                                                                                                                                                                                                                                                                                                                                      |
| 实时通知         | WebSocket 推送静默入列：「首页」未读角标累计 + 消息中心落库可回看（按「角色-id」隔离，同一台手机切换账号互不可见），点击消息直达对应功能页（学业预警提醒直达预警页）                                                                                                                                                                                                                                                                           |
| 加载体验         | 请求层计数式统一加载动画（并发请求蒙层自动叠加/收起），失败按「断网/超时/其他」细分提示；保存类操作防重复提交；列表只在首次进入加载，选图/预览后返回保持列表与滚动位置（离开超 5 分钟静默刷新）；首页切回不弹蒙层；表单弹层按钮固定在底部、字段多时只滚动表单区                                                                                                                                                                                |
| 小程序能力       | 新版本提示（发布新版后，冷启动时新版本下载完成即提示重启，取消则下次冷启动生效）；右上角「转发」：通知 / 考试安排 / 教室 / 课程页转发当前页，其余页面转发首页（不开放朋友圈：朋友圈打开是单页模式，无法登录）                                                                                                                                                                                                                                  |
| 跨端资料同步     | 头像等资料在任一端修改后，进入「我的」页自动拉取最新信息，无需重新登录                                                                                                                                                                                                                                                                                                                                                                         |

## 运行

```bash
npm install
npm run dev:mp-weixin     # 开发模式，产物在 dist/dev/mp-weixin
npm run build:mp-weixin   # 生产构建，产物在 dist/build/mp-weixin
```

用微信开发者工具"导入项目"选择对应的 `dist/.../mp-weixin` 目录即可（AppID 在 `src/manifest.json` 的 `mp-weixin.appid` 中填写，或使用测试号）。

### 在微信开发者工具里验证小程序专属能力

转发、新版本提示只在微信里有，H5 预览测不到：

- 转发：页面右上角「…」→「转发」应可点；通知 / 考试安排 / 教室 / 课程页转发的是当前页，其余页面转发首页
- 新版本提示：编译模式下拉 →「添加编译模式」→ 勾选「下次编译时模拟更新」，再编译一次即弹出更新提示

### 工程化命令

```bash
npm run lint        # ESLint 检查（不写文件）
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
   - 正式发布：改为已备案的 https 域名（构建时会校验，见「工程化」的发布前地址校验）
   - 电脑的局域网 IP 可能被路由器重新分配（表现为请求全部失败、WebSocket 报 `connection failed`）：用 `ipconfig` 查到新 IP 后更新两个 env 文件，并**重启** `npm run dev:mp-weixin`（env 只在启动时读取）；一劳永逸可在路由器里给电脑做 DHCP 静态分配
4. 数据库使用 Web 端仓库的最新种子 `manager-vue3/sql/xm_educational_manager-full.sql`（已含 `theme`/`locale` 列与 RBAC 授权，无需单独迁移脚本）。
5. 真机联调连不上/头像不显示时，按 `src/utils/config.ts` 头部的排错清单逐项检查；**改代码后需重新点「预览」生成新二维码**（旧二维码是当时代码的快照）。

## 目录结构

```text
src/
+-- api/                        # 接口层：业务接口路径全部集中在这里，页面只调函数（后端改路径只改一处）
|   +-- crud.js                 # createCrudApi：与后端 CrudController 的 7 个标准接口同名（selectPage / add / update / delete …）
|   +-- index.js                # 16 个 CRUD 模块 xxxApi（一个 Controller 一个，含统计 / 推荐 / 重置密码等额外接口）+ 学业预警 warningApi + userApiOf(role)
|   +-- account.js              # 账号与偏好：登录 / 注册 / 验证码 / 改密 / 权限码 / 语言、主题、主题色
+-- components/                 # easycom 业务组件（目录名即标签名，自动注册）
|   +-- xm-loader/              # 统一加载动画（追逐圆点，utils/loading.ts 状态订阅驱动）
|   +-- xm-search-card/         # 通用搜索卡（关键字 + 查询/重置 + filter 插槽）
|   +-- xm-action-bar/          # 列表工具条（总条数 / 批量管理 / 批量删除）+ 右下角「新增」悬浮按钮
|   +-- xm-form-popup/          # 通用底部弹层表单（保存中按钮转圈禁用、底部按钮固定、遮罩防背景滚动）
|   +-- xm-picker/              # 表单下拉（v-model：枚举 / 接口列表 / 日期三种用法，空值与失效值显示占位）
|   +-- xm-select-popup/        # 可搜索底部选择器（教室、教师等长列表，可叠在表单弹层之上）
|   +-- xm-icon/                # 线性图标（icons.js 图标表 + CSS mask，颜色跟随文字色）
|   +-- xm-list-footer/         # 列表底部反馈条（加载中/没有更多/加载更多）
|   +-- xm-empty/               # 通用空态（图标 + 文案 + 重新加载操作）
+-- i18n/index.js               # 轻量 i18n（词条键与 Web 端一致）
+-- locales/                    # zh-CN / en-US 核心包 + pages/ 分组包
+-- stores/
|   +-- user.ts                 # Pinia 用户状态（uni storage 持久化，密码类字段持久化前剔除；accountKey「角色-id」作本地数据的键）
|   +-- message.ts              # 消息中心仓库（推送按账号「角色-id」本地持久化、未读计数、关键词跳转）
+-- composables/
|   +-- useListPage.js          # 列表页标准骨架（搜索条件 + 重置 + 角色拦截 + 附属下拉加载 + 触底分页；16 个列表页共用）
|   +-- useCrud.js              # 通用 CRUD（分页列表/表单/删除确认/失败页码回滚/保存前后钩子/loadOnShow 返回不重载）
|   +-- useFocusChain.js        # 表单键盘「下一项」接力聚焦（登录/注册/改密）
|   +-- useManage.ts            # 批量管理模式（勾选/退出清空）
|   +-- useResetPassword.js     # 重置密码（确认 → PUT → 提示，三个用户管理页共用）
|   +-- useUserForm.js          # 用户表单：头像上传 + 保存后同步当前账号（按角色 + id 判断，三表 id 互不串号）
|   +-- usePermission.js        # RBAC 权限码拉取（菜单按权限过滤，与 Web 端一致）
|   +-- useTheme.js             # 主题三档切换 + 系统深色跟随 + 后端同步 + 原生导航/tabBar 联动
|   +-- useThemeColor.js        # 自定义主题色（按账号拉取/防抖保存、页面根节点 themeStyle、退出复位）
|   +-- useScreen.js            # 屏幕宽度（随旋转 / 窗口缩放更新）与运行期 rpx() 换算（宽屏封顶）
|   +-- useLocale.js            # 语言偏好 + 后端同步
|   +-- useTodayCourses.js      # 首页「今日课程」数据（学生取课表今日列 / 教师取本人今天的课）
+-- utils/                      # 核心层（渐进式 TS）
|   +-- config.ts               # 后端地址（头部附真机联调排错清单）
|   +-- request.ts              # uni.request 封装（token 头、验证码 Cookie、401 统一处理（并发防抖）、
|                               #   业务码解包 + ApiError、提示去重、计数式加载动画、断网/超时细分提示、头像地址归一化）
|   +-- websocket.ts            # 实时通知（静默入列落库 + 角标、心跳、入站静默检测、断线重连）
|   +-- loading.ts              # 加载状态订阅中心（计数 + 最短展示时长，xm-loader 挂载即同步）
|   +-- authGuard.js            # 登录拦截（路由拦截器 + ensureLoggedIn 页面兜底）
|   +-- errorReport.js          # 全局错误上报（微信实时日志，预期错误过滤 + 去重）
|   +-- themeColor.js           # 主题色纯函数（预设色板、混色算法、原生导航色，与 Web 端同算法）
|   +-- todaySchedule.js        # 今日课程纯函数（节次时间解析、上课状态、课表单元格拆分）
|   +-- courseColor.ts          # 课程块配色（课表周视图与首页今日课程共用）
|   +-- enums.js                # 业务枚举表（库存中文值 → 当前语言文案 / 标签色 / 下拉选项，新增枚举只改这一处）
|   +-- picker.js               # xm-picker 纯函数（当前下标、展示文案、空值判断）
|   +-- confirm.js              # Promise 版确认框：if (!(await confirm(文案))) return
|   +-- dateText.js             # 首页日期文案与按时段问候（中英文）
|   +-- rpxCap.js               # rpx 大屏封顶：构建期 PostCSS 插件 + 换算函数（vite.config.js 注册）
|   +-- upload.ts               # 选图 / 选聊天文件 / 上传（与请求层同契约：401、ApiError、进度、大小与类型本地校验）
|   +-- attachment.js           # 附件文件名 / 图片判断 / 查看（图片预览，文档下载后打开，失败复制链接）
|   +-- gpa.js                  # 成绩汇总纯函数（绩点换算规则集中在 gradePoint 一处，按课程学分加权）
|   +-- userCache.js            # 按账号（角色-id）隔离的本地缓存：首页 / 课表先渲染缓存再静默刷新
|   +-- appUpdate.js            # 小程序新版本提示（下载完成后提示重启）
|   +-- share.js                # 右上角「转发」规则（公开信息页转发当前页，其余转发首页）
+-- styles/theme.scss           # CSS 变量主题 + 通用 xm-* 组件类（卡片、标签、分段筛选 xm-tabs 等，对齐高星 uni-app 项目设计规范）
+-- static/tabbar/              # 底部 Tab 图标（常态 / 选中态）
+-- pages/                      # 19 个主包页面（login 为入口；含消息中心、学业预警）
|   +-- home/components/        # 首页区块组件：头卡 / 功能入口 / 今日课程 / 动态 / 统计（首页本体只管数据与缓存）
|   +-- course/                 # components/course-form.vue 课程表单（含教室 / 授课教师可搜索选择器）；room.js 排课教室公共逻辑
|   +-- homework/               # components/homework-form.vue 作业提交表单（含附件上传）
+-- pages-admin/                # 6 个管理页（分包 + 首页 wifi 预载）
tests/                          # Vitest 单元测试（25 个文件 192 条，setup.js 提供全局 uni mock）
```

## 二次开发：新增一个列表页

16 个列表页共用同一套骨架：脚本调用一次 `useListPage`，模板用 `xm-*` 组件拼装，业务枚举在 `utils/enums.js` 定义一次。以新增「教材信息」为例（假设后端按现有模块的约定提供了 `/textbook/selectPage | add | update | delete/{id} | delete/batch`）：

**1. 脚本**：分页、搜索与重置、表单保存、删除确认、批量管理、下拉刷新、触底加载、登录拦截都由 `useListPage` 完成，页面只写业务差异

```js
import { ref } from 'vue'
import { useListPage } from '@/composables/useListPage'
import { textbookApi, courseApi } from '@/api'
import { orNull, SILENT } from '@/utils/request'
import { enumLabel, enumOptions } from '@/utils/enums'
import { t } from '@/i18n'

const courseData = ref([])

// 按模板用到的解构即可
const {
  list,
  loading,
  finished,
  total,
  query,
  form,
  formVisible,
  saving,
  selectedIds,
  manageMode,
  load,
  loadNext,
  search,
  resetQuery,
  toggleManage,
  toggleSelect,
  handleAdd,
  handleEdit,
  closeForm,
  save,
  del,
  delBatch,
} = useListPage({
  api: textbookApi, // 接口对象（src/api）：分页 / 新增 / 修改 / 删除都走它
  title: 'menu.textbook', // 导航栏标题词条（切换语言后自动更新）
  roles: ['ADMIN'], // 可选：只允许这些角色进入，其他角色提示无权限并返回
  query: { name: '' }, // 搜索条件初值，自动作为请求参数（空值不传）
  loadExtras: async () => {
    // 可选：随列表一起加载的下拉数据
    courseData.value = (await orNull(courseApi.selectAll(undefined, SILENT))) || []
  },
  validate: (f) => (f.name ? '' : t('pages.textbook.ruleNameRequired')),
})
```

**2. 模板**：根节点绑定主题；卡片列表在手机单列、平板自动两列 / 三列

```html
<view
  class="xm-page"
  :class="themeClass"
  :style="themeStyle"
>
  <xm-search-card
    v-model="query.name"
    :placeholder="$t('pages.textbook.searchPlaceholder')"
    @search="search"
    @reset="resetQuery"
  />
  <xm-action-bar
    :manage-mode="manageMode"
    :total="total"
    :selected-count="selectedIds.length"
    @add="handleAdd()"
    @toggle-manage="toggleManage"
    @del-batch="delBatch"
  />
  <xm-empty
    v-if="!list.length && !loading"
    :action-text="$t('common.reload')"
    @action="load(true)"
  />

  <view class="xm-list">
    <view
      v-for="item in list"
      :key="item.id"
      class="xm-card"
    >
      <checkbox
        v-if="manageMode"
        :checked="selectedIds.includes(item.id)"
        @click.stop="toggleSelect(item.id)"
      />
      <text class="xm-card-name">{{ item.name }}</text>
      <text class="xm-tag">{{ enumLabel('textbookType', item.type) }}</text>
      <view
        v-if="!manageMode"
        class="xm-actions"
      >
        <button
          class="xm-btn xm-btn-plain"
          @click="handleEdit(item)"
        >
          {{ $t('common.edit') }}
        </button>
        <button
          class="xm-btn xm-btn-danger"
          @click="del(item.id)"
        >
          {{ $t('common.delete') }}
        </button>
      </view>
    </view>
  </view>
  <xm-list-footer
    :visible="!!list.length"
    :loading="loading"
    :finished="finished()"
    @load-more="loadNext"
  />

  <xm-form-popup
    :visible="formVisible"
    :saving="saving"
    @close="closeForm"
    @save="save"
    :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.textbook.entity') })"
  >
    <view class="xm-form-item">
      <view class="xm-form-label">{{ $t('pages.textbook.name') }}</view>
      <input
        class="xm-input"
        v-model="form.name"
      />
    </view>
    <view class="xm-form-item">
      <view class="xm-form-label">{{ $t('pages.textbook.type') }}</view>
      <!-- 枚举下拉 -->
      <xm-picker
        v-model="form.type"
        :options="enumOptions('textbookType')"
        :placeholder="$t('pages.textbook.typePlaceholder')"
      />
    </view>
    <view class="xm-form-item">
      <view class="xm-form-label">{{ $t('pages.textbook.course') }}</view>
      <!-- 接口列表下拉：原始数组直接传入，指定文案 / 取值字段 -->
      <xm-picker
        v-model="form.courseId"
        :options="courseData"
        label-key="name"
        value-key="id"
      />
    </view>
    <view class="xm-form-item">
      <view class="xm-form-label">{{ $t('pages.textbook.publishDate') }}</view>
      <xm-picker
        v-model="form.publishDate"
        mode="date"
      />
    </view>
  </xm-form-popup>
  <xm-loader />
</view>
```

**3. 注册清单**

- 接口：`src/api/index.js` 加一行 `export const textbookApi = createCrudApi('/textbook')`；后端有额外接口时在同一个对象里补充（方法名与后端一致），页面里不写接口路径
- `src/pages.json` 登记页面并开启下拉刷新（刷新逻辑已由骨架统一处理）：`{ "path": "pages/textbook/textbook", "style": { "navigationBarTitleText": "教材信息", "enablePullDownRefresh": true } }`；仅管理员使用的页面放 `pages-admin` 分包
- 词条：`menu.textbook` 与 `pages.textbook.*` 在 `zh-CN` / `en-US` 两个包里都要加（缺键时界面直接显示键名）
- 新枚举：`src/utils/enums.js` 加一组 `{ value: 库存中文值, label: 词条键, tag: 标签色（可选） }`，列表文案 `enumLabel`、标签色 `enumTag`、下拉 `enumOptions` 三处自动一致
- 首页入口：`src/pages/home/components/home-quick-entry.vue` 的入口表加一行：`{ path, name: 'menu.textbook', icon, perm: 'textbook:view', roles: ['ADMIN'] }`（`perm` 是后端 RBAC 权限码，需在权限表授权；`roles` 可选）

**4. 常见变体**（都有现成页面可参考）

| 需求                          | 写法                                                                                                                              | 参考页面                                   |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| 删除确认用专门文案            | `deleteConfirm: 'pages.xxx.deleteConfirm'`                                                                                        | 选课、请假、作业、成绩                     |
| 编辑时重置部分字段            | `handleEdit(row, { status: '待审核', descr: '' })`                                                                                | 请假、作业                                 |
| 保存前补字段 / 异步校验       | `beforeSave: (f) => { … }`，返回非空字符串即提示并中止保存                                                                        | 考勤、成绩、课程（自动分配教室）           |
| 保存后联动                    | `afterSave: syncCurrentUser('TEACHER', ['avatar', 'name'])`                                                                       | 用户管理                                   |
| 下拉联动（选课程 → 加载学生） | `<xm-picker @change="(course) => loadStudents(course.id)" />`，change 回传整条选项                                                | 考勤、成绩                                 |
| 状态 Tab 快捷筛选             | 通用样式 `.xm-tabs` / `.xm-tab`；后端筛选写 `query.status = value; search()`，一次拉全的列表直接在前端过滤                        | 请假（后端筛选）、学业预警（前端筛选）     |
| 几百项的长列表选择            | `xm-select-popup`（带搜索）                                                                                                       | 课程（教室 / 教师）                        |
| 其他确认操作                  | `if (!(await confirm(t('…'), { title: t('…') }))) return`                                                                         | 消息中心、我的                             |
| 重置密码                      | `const resetPassword = useResetPassword(teacherApi, 'teacher')`                                                                   | 用户管理                                   |
| 非通用 CRUD 的接口            | 在 `src/api/index.js` 单独定义对象，如 `warningApi = { list, notify }`                                                            | 学业预警                                   |
| 页面秒开 / 断网可看           | 进入时 `readUserCache(名称, userStore.accountKey)` 先渲染，刷新成功后 `writeUserCache` 写回（按账号隔离）                         | 首页、我的课表                             |
| 允许转发当前页                | `src/utils/share.js` 的 `SHARE_PAGES` 加一行（路由 → 标题词条），其余页面默认转发首页                                             | 通知、考试安排                             |
| 推送消息点开跳到该页          | `src/stores/message.ts` 的 `open()` 加关键词（按先后顺序匹配，关键词重叠时把更具体的放前面）                                      | 学业预警                                   |
| 表单字段多 / 逻辑重           | 拆成页面私有组件（`pages/xxx/components/`）：父页面 `v-model:form="form"`，组件里 `const form = defineModel('form')` 直接编辑字段 | 课程（course-form）、作业（homework-form） |

> 语言包的键与 Web 端保持一致：表格列名、弹窗标题等 Web 端专用的键移动端用不到，保留是为了两端文案同步，清理时不要当作死代码删除。

## 与 Web 端的交互适配说明

功能一致的前提下，交互形式按移动端习惯做了等价适配：

- 表格 → 卡片列表 + 底部弹层表单；分页 → 上拉/点击"加载更多"。
- 批量删除 → "管理"模式勾选后批量删除（管理员角色）。
- ECharts 图表 → 小程序端首页统计使用**纯 CSS 图表**（conic-gradient 环形图 + 柱状图），数据来源接口不变：`/attendance/getPie`、`/score/getLine`；不引入 ECharts 的原因见「已知取舍」。
- 下拉 / 日期控件 → 原生选择器（统一经 `xm-picker` 封装，日期为 `mode="date"`，后端仍按字符串存储，两端行为一致）；附件选择 → `uni.chooseImage` + `uni.uploadFile`。
- 页面加载 → 统一 `xm-loader` 加载动画蒙层（首屏/搜索时出现，触底加载静默由列表底部反馈）。
- 学业预警表格 → 卡片（风险指数进度条 + 四项指标 + 建议），等级筛选改为顶部分段标签（附人数）。
- 成绩页课程筛选：移动端按角色取下拉数据（学生取已选课程、管理员取全部、教师取本人课程）；Web 端目前统一按 `teacherId = 当前用户 id` 取，学生 / 管理员拿到的列表不对，建议 Web 端同步修正。

## 刻意保留中文的部分（与 Web 端一致）

数据库枚举值（考勤状态、请假状态、课程性质、上课时段等）按中文存储，界面仅翻译展示层文案；后端统计按中文值分组，两端行为一致。学业预警的等级（高风险 / 中风险 / 低风险 / 正常）同样按中文返回、界面翻译；预警建议文字由后端生成，英文界面下仍显示中文（与 Web 端一致）。

## 已知取舍（生产化改造方向）

- **统计图表使用纯 CSS 而非 ECharts**：ECharts 依赖浏览器全局（window/document）与 canvas 环境，npm echarts 6.x 与 UMD 构建在小程序逻辑层均存在模块加载/渲染兼容问题（组件文件丢失、module not defined 等）。纯 CSS 图表零依赖、渲染确定性 100%；如后续必须使用 ECharts，建议引入 lime-echart 并完整回归真机。
- **教室时段占用为应用层校验**：同一「教室 + 星期 + 大节」的唯一性由保存时校验（5010）与教室唯一编号（5011）保证，数据库层未建组合唯一索引——因「已结课释放」语义与唯一索引冲突；极端并发下存在理论双写窗口，如需强一致可引入排课事务或历史课归档表。
- WebSocket 鉴权把 token 拼在 URL 路径（`/ws/notice/{token}`）：会进服务器访问日志，生产化应改为「连接后首条消息鉴权」，需与后端同步调整。
- 教师课程编辑权限：TEACHER 新增了 `course:manage` 授权（业务层在 CourseService 限制教师仅能改本人课程的教室/时间/状态，不可增删）。**老库升级需手动执行授权**：`INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2, 9);`
- 登录态无 refresh token 机制，token 过期统一按 401 踢回登录页。
- **考试倒计时暂未做**：考试安排表只有 `time` 一个日期字段，数据库注释与两端表单都是「创建时间」（发布时间），不是考试时间；要做需后端给考试安排加「考试时间」字段（数据库 + 实体 + Web / 小程序表单），再在列表和首页显示倒计时。
- **绩点换算规则**：默认（成绩 − 50）÷ 10（60 分 1.0、不及格 0），按课程学分加权，缺学分的课程只计门数不参与加权；各校规则不同，改 `src/utils/gpa.js` 的 `gradePoint` 一处即可。
- **本地数据改为按账号存储**：首页缓存与消息历史的键由「id」改为「角色-id」（三类账号 id 会重复，只用 id 会让 1 号管理员看到 1 号学生的推送）；旧版本只按 id 存的本地消息历史不做迁移（分不清属于哪个角色），升级后不再显示。
- **大屏适配的边界**：宽屏尺寸封顶靠构建期插件为 rpx 规则生成 px 覆盖，模板里写死的内联 rpx（`style="…rpx"`）插件处理不到，新写的内联尺寸请改用 class 或 `useScreen` 的 `rpx()`；原生导航栏 / tabBar 由微信绘制，不受页面布局影响。
- **主题色的边界**：小程序没有原生取色器，移动端只提供预设色板（Web 端可选任意颜色）；tabBar 图标是预先绘制的图片（已按应用内线性图标风格重绘），无法运行时改色，自定义主题色时图标保持默认靛蓝、仅文字跟随（改用自定义 tabBar 可跟随主题色，但 `setTabBarBadge` 等原生角标接口会失效，消息未读角标需重写，暂未采用）；小程序无法在运行时改 `page` 根样式，主题色通过各页面根节点 `:style="themeStyle"` 注入 CSS 变量，**新增页面需同样在根节点绑定**。
- **登录拦截需页面配合**：路由拦截器拦不到原生 tabBar 点击、冷启动与分享进入，页面要在 `onShow` 里兜底：用 `useListPage` 的列表页已自动处理，其他新增业务页需在 `onShow` 开头调用 `if (!ensureLoggedIn()) return`（登录/注册页除外）。
