# 教务管理系统 - 微信小程序版

与 Web 端（`manager-vue3`）功能对齐的微信小程序版本，基于 **uni-app（Vue 3 + Vite）** 开发，连接同一个 Spring Boot 后端（`manager-vue3/springboot`，端口 9091）。

## 技术栈

- uni-app（Vue 3 组合式 API + Vite），编译目标 `mp-weixin`
- Pinia（用户状态，与 Web 端同构）
- 自研轻量 i18n（词条键与 Web 端 vue-i18n 完全一致，中/英双语）
- CSS 自定义属性主题系统（浅色/深色/跟随系统，与 Web 端共用后端偏好）
- WebSocket 实时通知（`uni.connectSocket`，心跳保活 + 断线指数退避重连，与 Web 端同一推送通道）
- rpx 弹性布局 + `@media` 断点：手机 3 列宫格 / 平板 4 列 / 宽屏 6 列（内容区限宽居中），适配不同设备宽度

## 功能对照（与 Web 端一致）

| 模块 | 说明 |
|---|---|
| 登录/注册/验证码 | 图形验证码 + Session Cookie 手动维护；学生自助注册 |
| 首页 | 欢迎、按角色的功能宫格、教务通知、考试安排、考勤统计、成绩统计；数据本地缓存秒开（按用户隔离）+ 无缓存时骨架屏 |
| 信息公告 | 教务通知 / 考试安排 / 教室安排（管理员维护，长文本展开查看） |
| 行政管理 | 学院 / 专业 / 班级（仅管理员；含学院→专业级联下拉） |
| 教学管理 | 课程（含学生选课）、我的选课（取消选课/评教）、我的课表、我的成绩（教师录入）、网上评教 |
| 教务管理 | 请假申请（学生申请/撤销、管理员审核）、作业提交（含附件上传、教师打分）、考勤信息 |
| 用户管理 | 管理员/教师/学生管理（含重置密码、头像上传、级联归属选择） |
| 个人中心 | 个人信息（按角色差异化字段 + 头像上传）、修改密码 |
| 主题 | 浅色/深色/跟随系统三档，登录后与后端同步（账号表 `theme` 列） |
| 语言 | 中文/English 切换，登录后与后端同步（账号表 `locale` 列） |
| 实时通知 | WebSocket 推送（成绩发布/作业批改/请假审核/教务通知）：toast 提醒 + 「首页」未读角标，进入首页即清零 |
| 跨端资料同步 | 头像等资料在任一端修改后，进入「我的」页自动拉取最新信息，无需重新登录 |

## 运行

```bash
npm install
npm run dev:mp-weixin     # 开发模式，产物在 dist/dev/mp-weixin
npm run build:mp-weixin   # 生产构建，产物在 dist/build/mp-weixin
```

用微信开发者工具"导入项目"选择对应的 `dist/.../mp-weixin` 目录即可（AppID 在 `src/manifest.json` 的 `mp-weixin.appid` 中填写，或使用测试号）。

### 连接后端

1. 启动 Web 端的 Spring Boot 后端（端口 9091）。
2. 开发者工具中勾选 **详情 → 本地设置 → 不校验合法域名**（`urlCheck` 已在 manifest 中默认关闭）。
3. 接口地址在 `.env.development` / `.env.production` 的 `VITE_API_BASE_URL`（由 `src/utils/config.js` 读取，改环境只改 env 文件）：
   - 开发者工具模拟器：`http://localhost:9091`
   - 真机调试：改为电脑的局域网 IP（如 `http://192.168.x.x:9091`），手机与电脑同一 Wi-Fi。
4. 数据库使用 Web 端仓库的最新种子 `manager-vue3/sql/xm_educational_manager-full.sql`（已含 `theme`/`locale` 列与 RBAC 授权，无需单独迁移脚本）。
5. 真机联调连不上/头像不显示时，按 `src/utils/config.js` 头部的排错清单逐项检查；**改代码后需重新点「预览」生成新二维码**（旧二维码是当时代码的快照）。

## 目录结构

```text
src/
+-- i18n/index.js               # 轻量 i18n（词条键与 Web 端一致）
+-- locales/                    # zh-CN / en-US 核心包 + pages/ 分组包
+-- stores/user.js              # Pinia 用户状态（uni storage 持久化）
+-- composables/
|   +-- useCrud.js              # 通用 CRUD（分页列表/表单/删除确认，与 Web 端语义对齐）
|   +-- usePermission.js        # RBAC 权限码拉取（菜单按权限过滤，与 Web 端一致）
|   +-- useTheme.js             # 主题三档切换 + 系统深色跟随 + 后端同步 + 原生导航/tabBar 联动
|   +-- useLocale.js            # 语言偏好 + 后端同步
+-- utils/request.js            # uni.request 封装（token 头、验证码 Cookie、401 统一处理、头像地址归一化）
+-- utils/websocket.js          # 实时通知（推送 toast/角标、心跳、断线重连，兼容各端 connectSocket 返回形态）
+-- utils/config.js             # 后端地址（头部附真机联调排错清单）
+-- styles/theme.scss           # CSS 变量主题 + 通用 xm-* 组件类（对齐高星 uni-app 项目设计规范）
+-- pages/                      # 23 个页面（login 为入口，未登录自动跳转）
```

## 与 Web 端的交互适配说明

功能一致的前提下，交互形式按移动端习惯做了等价适配：

- 表格 → 卡片列表 + 底部弹层表单；分页 → 上拉/点击"加载更多"。
- 批量删除 → "管理"模式勾选后批量删除（管理员角色）。
- ECharts 图表 → 纯 CSS 占比条形统计（数据来源接口不变：`/attendance/getPie`、`/score/getLine`）。
- 日期控件 → 文本输入（后端按字符串存储）；附件选择 → `uni.chooseImage` + `uni.uploadFile`。

## 刻意保留中文的部分（与 Web 端一致）

数据库枚举值（考勤状态、请假状态、课程性质、上课时段等）按中文存储，界面仅翻译展示层文案；后端统计按中文值分组，两端行为一致。
