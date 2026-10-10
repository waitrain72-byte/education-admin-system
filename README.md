# 教务管理系统 · 微信小程序版

与 Web 端（课程空间版）同步的微信小程序，基于 **uni-app（Vue 3 + Vite）** 开发，连接同一个 Spring Boot 后端（端口 9091）。
管理员 / 教师 / 学生三类角色，中英文切换，浅色 / 深色 / 跟随系统三档主题，可换主题色；语言、主题、主题色按账号存在后端，两端互通。

## 和 Web 端怎么对应

Web 端按「课程空间」组织，顶部导航是「首页 / 课程 / 日程 / 消息」。小程序把它们放进底部五个 tab：

| 小程序 tab | 对应 Web 端                          | 说明                                                           |
| ---------- | ------------------------------------ | -------------------------------------------------------------- |
| 首页       | 首页                                 | 学生、教师、管理员各一版                                       |
| 课程       | 课程卡片，以及课程广场、成绩单的入口 | 点进一门课就是课程空间（分包 `pages-course`）                  |
| 日程       | 日程：周课表 / 月历 / 请假           | 管理员这个位置是「后台」，即教务后台入口（分包 `pages-admin`） |
| 消息       | 消息中心、通知                       | 未读数显示在 tab 角标上                                        |
| 我的       | 顶栏的用户菜单                       | 个人信息、修改密码、外观（主题 / 主题色 / 语言）、退出登录     |

小程序的 tabBar 不能按角色换成别的页面，所以管理员的第三个 tab 还是同一个页面（`pages/schedule/schedule`），只换了文字和图标，页面按角色显示日程或后台入口。

文案与 Web 端共用一套词条键：`src/locales/pages/` 下的 groupA–C、shell、space、campus、console 由 Web 端同名文件转换而来，小程序独有的词条放在 `mobile.js`。其中有些键只有 Web 端页面用得到，保留是为了两端整份同步，清理时不要当成死代码删掉。

## 功能

**登录 / 注册**：只填账号、密码、验证码，系统自动识别身份；同一个账号对应多个身份时（错误码 5012）再选一次身份。学生可以自助注册，教师和管理员账号由教务后台创建。

**学生**

- 首页：问候与当前教学周、下一节课、今天的课、学分进度与平均学分绩点、快到期的作业、近期考试（带倒计时标签）、学业预警提示
- 课程：已选课程（按状态筛选 + 搜索）；课程广场选课 / 退选（「猜你想选」、只看有名额、按类型 / 星期筛选，名额、时间冲突、已结课由后端校验并提示）；成绩单（已修学分、平均学分绩点、各科总评）
- 课程空间：签到（输入老师给的 4 位签到码）、交作业（文字 + 附件，截止前且还没批改时可以改）、看成绩（老师发布后可见）、课程结束后五个维度打分评价、下载资料、看公告和成员
- 日程：周课表 / 月历，考试、作业截止、请假也标在上面；在月历上选日期请假，提交前列出会缺的课；待审核的申请可以撤回

**教师**

- 首页：今天的课、待批改的作业、需要关注的学生、所授课程
- 课程空间：发起签到（签到码 + 倒计时，学生签到实时刷新）、逐个调整考勤状态；布置作业、逐份批改（分数 + 评语，改完自动跳到下一份）；成绩册（四项权重、录平时分 / 期末分、总评预览、存草稿 / 发布）；看匿名评价汇总；上传资料；发公告、改课程简介

**管理员**

- 首页：本学期指标、待审批的请假、还没排课的课程（点开直接进排课表单）、最新通知
- 教务后台（第三个 tab）：课程管理与三步开课向导（时间与教室一步只列出该时段空着、坐得下的教室）、教室与每周占用、请假审批、考试安排、学业预警（可一键提醒）、组织架构（学院 → 专业 → 班级逐级进入，点班级看学生）、人员（学生 / 教师 / 管理员的增删改、按组织筛选、重置密码）、学期设置
- 消息页可以发布、编辑、删除通知（有 `notice:manage` 权限的账号才显示）
- 选课 / 成绩 / 考勤 / 作业 / 评价等「教学记录」表格、权限设置、日志、数据大屏、Excel 导入导出只在 Web 端，后台页底部有提示

**通用**

- 消息中心：全部 / 未读筛选、一键已读、删除；点消息跳到对应页面（后端给的是 Web 端路由，由 `utils/link.js` 换算成小程序页面）
- 实时推送：成绩发布、作业批改、请假审核、学业预警、课程公告到达时刷新未读角标；有人签到、交作业、发公告时，打开着的课程空间自动刷新
- 外观：主题三档、8 个预设主题色（Web 端选的任意颜色在小程序也生效，标为「自定义」）、中文 / English；导航栏和 tabBar 跟着主题变

## 运行

```bash
npm install
npm run dev:mp-weixin     # 开发模式，产物在 dist/dev/mp-weixin
npm run build:mp-weixin   # 生产构建，产物在 dist/build/mp-weixin
```

用微信开发者工具「导入项目」，目录选上面的产物目录（不是源码根目录）。AppID 填在 `src/manifest.json` 的 `mp-weixin.appid`，没有的话可以先用测试号。

### 连接后端

1. 按 Web 端 README 启动后端（`springboot/`，端口 9091），数据库导入 Web 端仓库的 `sql/xm_educational_manager-full.sql`（库名 `xm_educational_manager_v2`）。
2. 接口地址在 `.env.development` / `.env.production` 的 `VITE_API_BASE_URL`（`src/utils/config.ts` 读取）：
   - 开发者工具模拟器：`http://localhost:9091`
   - 真机预览：电脑的局域网 IP（如 `http://192.168.x.x:9091`），手机和电脑连同一个 Wi-Fi；IP 变了要改 env 文件并**重启** `npm run dev:mp-weixin`（env 只在启动时读取）
   - 正式发布：已备案的 https 域名，用 `RELEASE=1 npm run build:mp-weixin` 构建（地址不是 https、是局域网 / 本机地址或带端口时直接构建失败）；公众平台「开发管理 → 服务器域名」里登记 request / uploadFile / downloadFile 的 `https://` 地址和 socket 的 `wss://` 地址
3. 开发者工具勾选 **详情 → 本地设置 → 不校验合法域名**（manifest 里 `urlCheck` 已关闭）。
4. 真机连不上时按 `src/utils/config.ts` 头部的排错清单逐项检查；改了代码要重新点「预览」生成新二维码。

### 在开发者工具里验证小程序独有的能力

H5 预览测不到这些，要在开发者工具或真机上看：

- 附件来源：交作业、传资料时可以从「微信聊天记录」选 PDF / Word / Excel 等文件，也可以从相册选图片
- 转发：课程广场转发的是当前页，其余页面转发首页
- 新版本提示：编译模式下拉 →「添加编译模式」→ 勾选「下次编译时模拟更新」，再编译一次就会弹出更新提示

## 工程化

```bash
npm run lint        # ESLint 检查
npm run lint:fix    # ESLint 自动修复
npm run format      # Prettier 格式化 src
npm test            # Vitest 单元测试
npm run test:watch  # Vitest 监听模式
```

提交时 husky + lint-staged 自动对暂存文件跑 `eslint --fix` 和 `prettier --write`。

单元测试 21 个文件、182 个用例（`tests/setup.js` 提供全局 `uni` mock）：

| 范围                                         | 用例 | 内容                                                                                                   |
| -------------------------------------------- | ---- | ------------------------------------------------------------------------------------------------------ |
| `request` / `api` / `upload` / `attachment`  | 50   | 请求层契约（401、断网、超时、加载计数、`ApiError`、提示去重）；接口路径与参数；上传与选附件；附件查看  |
| `websocket` / `message`                      | 23   | 建连、心跳、半开连接检测、退避重连；有标题的推送刷新未读数，静默事件转发给课程空间；未读角标           |
| `schedule` / `courseSpace` / `examCountdown` | 32   | 节次解析、上课状态与下一节课、课程配色、日期计算；签到倒计时、出勤率、成绩册权重与总评预览；考试倒计时 |
| `link` / `tabbar` / `share`                  | 21   | Web 路由换算成小程序页面；五个 tab 与管理员的「后台」；转发规则                                        |
| `themeColor` / `useTheme` / `i18n`           | 27   | 主题色派生（与 Web 端同色同值）、原生导航色；系统深色读取；词条、插值、错误码本地化                    |
| `authGuard` / `accountScope` / 其他          | 29   | 登录与角色拦截、按账号隔离的本地缓存、错误上报、新版本提示、rpx 大屏封顶、表单接力聚焦                 |

## 目录结构

```text
src/
+-- api/                    # 接口层：页面只调这里的函数，不写路径
|   +-- index.js            # 首页、消息、课程与课程空间（签到 / 作业 / 成绩册 / 评价 / 资料）、日程、请假、成绩单、人员与组织架构
|   +-- account.js          # 登录、注册、验证码、改密、权限码、语言 / 主题 / 主题色偏好
|   +-- crud.js             # createCrudApi：后端 CrudController 的 7 个标准接口
+-- components/             # easycom 组件（目录名即标签名）
|   +-- xm-auth-shell/      # 登录 / 注册页外框（品牌区 + 语言切换）
|   +-- xm-course-card/     # 课程卡片（课程色条、上课时间、教室）
|   +-- xm-chips/ xm-search/ xm-empty/ xm-list-footer/ xm-loader/
|   +-- xm-form-popup/      # 底部弹层表单（按钮固定在底部，保存中禁用）
|   +-- xm-datetime/        # 日期 + 时间两个原生选择器拼成 yyyy-MM-dd HH:mm
|   +-- xm-org-picker/      # 学院 / 专业 / 班级级联选择
|   +-- xm-file-picker/ xm-file-chip/   # 选附件并上传（带进度）/ 附件查看
|   +-- xm-rate/ xm-progress-ring/ xm-user-avatar/ xm-brand-mark/ xm-icon/
+-- composables/            # useTheme / useThemeColor / useLocale / usePermission / usePager / useServerClock / useCourseEvents …
+-- stores/                 # user.ts（登录态、accountKey「角色-id」）、message.ts（未读数与 tab 角标）
+-- utils/
|   +-- request.ts upload.ts websocket.ts loading.ts config.ts
|   +-- schedule.ts         # 节次、星期、教学周、日期计算（与 Web 端同规则）
|   +-- courseSpace.ts      # 课程空间纯函数：签到倒计时、考勤状态、成绩册权重与总评预览
|   +-- courseColor.ts courseText.js examCountdown.js
|   +-- link.js             # 站内链接（Web 路由）→ 小程序页面
|   +-- tabbar.js           # 五个 tab、管理员的「后台」、选中图标
|   +-- themeColor.js       # 主题色派生（与 Web 端同算法）
|   +-- authGuard.js errorReport.js appUpdate.js share.js userCache.js attachment.js confirm.js rpxCap.js
+-- locales/                # zh-CN / en-US 核心包 + pages/ 分组包（与 Web 端同名同结构）+ mobile.js
+-- styles/theme.scss       # 主题变量（玉绿配色，深色模式）+ 通用 xm-* 样式类
+-- static/tabbar/          # tab 图标：未选中 / 选中（玉绿、深色提亮）/ 自定义主题色时的中性墨色
+-- pages/                  # 主包：login register home courses schedule messages mine person password
+-- pages-course/           # 分包：space（课程空间 7 个分页）assignment square transcript
+-- pages-admin/            # 分包：courses course-form rooms leaves exams warnings org people semester
tests/                      # Vitest 单元测试
```

`pages-course` 在进入「课程」tab 时预下载，`pages-admin` 在进入第三个 tab 时预下载。

## 实现要点

- **请求层**：`code === '200'` 直接返回业务数据，其他情况统一提示（同一句 2 秒内不重复）并以 `ApiError` reject，页面只写成功分支；401 清空登录态回登录页；请求计数驱动统一加载动画 `xm-loader`
- **服务器时间**：首页与日程判断「正在上课」「下一节」都用后端返回的时间往前推（`useServerClock`），手机时区、时间不准都不影响；签到倒计时同理
- **课程空间**：7 个分页首次打开时才加载，之后用 `v-show` 保留，切走再回来不丢成绩册里没保存的输入；成绩册有未保存的修改时返回会先确认；从消息点进来时指定的分页，要等确认了当前账号和这门课的关系才打开（和这门课无关的人不会被带进成绩册）
- **实时推送**：`uni.connectSocket` + 心跳保活 + 半开连接检测 + 指数退避重连；token 拼在路径里（`/ws/notice/{token}`）；有标题的推送刷新未读数，不带标题的课程事件经 `uni.$emit('ws:course')` 通知课程空间（400ms 内合并成一次刷新）
- **主题**：页面根节点绑定 `themeClass` / `themeStyle`（全局混入）；小程序开启 `darkmode` + `src/theme.json`，跟随系统时冷启动即是正确的深浅色；原生导航栏、tabBar 文字与图标逐页同步
- **登录拦截**：`uni.addInterceptor` 拦截路由跳转，tab 页与冷启动由页面 `onShow` 里的 `ensureLoggedIn()` 兜底；后台页面用 `ensureRole(['ADMIN'])`
- **宽屏适配**：手机按 rpx 等比缩放；超过 414px 由构建期插件（`src/utils/rpxCap.js`）把 rpx 换成固定 px，平板 / PC 上元素不再放大；≥720px 列表两列、弹层居中限宽，≥1248px 列表三列
- **本地数据按账号隔离**：首页缓存的键是「角色-id」（三类账号 id 会重复），同一台手机换账号不串数据；首页先显示缓存再静默刷新

## 二次开发约定

- **新页面**：根节点 `<view class="xm-page" :class="themeClass" :style="themeStyle">`；`onShow` 开头 `if (!ensureLoggedIn()) return`（管理员页面用 `ensureRole(['ADMIN'])`）；在 `src/pages.json` 登记，管理员页面放 `pages-admin` 分包
- **词条**：中英文两个包都要加，与 Web 端共用的键保持同名同文案；数据库里按中文存的枚举值（星期、大节、考勤状态、请假状态等）只翻译显示文字，不改入库值
- **消息跳转**：后端新增了带跳转地址的消息类型时，在 `src/utils/link.js` 里补上 Web 路由到小程序页面的换算，并在 `tests/link.spec.js` 加用例
- **转发**：允许转发当前页的页面加到 `src/utils/share.js` 的 `SHARE_PAGES`，其余页面默认转发首页
- **主题色预设**：`src/utils/themeColor.js` 的 `PRESET_COLORS` 与 Web 端 `useThemeColor.ts` 各有一份，改的时候两边一起改

## 已知取舍

- 小程序没有原生取色器，主题色只提供预设色板（Web 端可以任意取色）。tabBar 图标是图片，不能运行时改色：自定义了主题色时，选中的图标换成中性墨色版，文字跟随主题色。
- 课程资料、作业附件在微信里只能从聊天记录或相册选（微信小程序不能直接选手机里的文件）；单个文件不超过 20MB，类型与后端白名单一致。
- WebSocket 的 token 在 URL 路径里，会进服务器访问日志；生产化应改为连接后首条消息鉴权，需要和后端一起改。
- 登录态没有 refresh token，过期后统一按 401 回登录页。
- 宽屏封顶靠构建插件处理样式里的 rpx，模板里写死的内联 rpx 处理不到；新写的内联尺寸用 class 或 `useScreen` 的 `rpx()`。
