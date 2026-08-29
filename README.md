# 教务管理系统 - 微信小程序版

与 Web 端（`manager-vue3`）功能对齐的微信小程序版本，基于 **uni-app（Vue 3 + Vite）** 开发，连接同一个 Spring Boot 后端（`manager-vue3/springboot`，端口 9091）。

## 技术栈

- uni-app（Vue 3 组合式 API + Vite），编译目标 `mp-weixin`
- Pinia（用户状态，与 Web 端同构）
- 自研轻量 i18n（词条键与 Web 端 vue-i18n 完全一致，中/英双语）
- CSS 自定义属性主题系统（浅色/深色/跟随系统，与 Web 端共用后端偏好）
- rpx 弹性布局 + `@media` 断点：手机 3 列宫格 / 平板 4 列 / 宽屏 6 列（内容区限宽居中），适配不同设备宽度

## 功能对照（与 Web 端一致）

| 模块 | 说明 |
|---|---|
| 登录/注册/验证码 | 图形验证码 + Session Cookie 手动维护；学生自助注册 |
| 首页 | 欢迎、按角色的功能宫格、教务通知、考试安排、考勤统计、成绩统计 |
| 信息公告 | 教务通知 / 考试安排 / 教室安排（管理员维护，长文本展开查看） |
| 行政管理 | 学院 / 专业 / 班级（仅管理员；含学院→专业级联下拉） |
| 教学管理 | 课程（含学生选课）、我的选课（取消选课/评教）、我的课表、我的成绩（教师录入）、网上评教 |
| 教务管理 | 请假申请（学生申请/撤销、管理员审核）、作业提交（含附件上传、教师打分）、考勤信息 |
| 用户管理 | 管理员/教师/学生管理（含重置密码、头像上传、级联归属选择） |
| 个人中心 | 个人信息（按角色差异化字段 + 头像上传）、修改密码 |
| 主题 | 浅色/深色/跟随系统三档，登录后与后端同步（账号表 `theme` 列） |
| 语言 | 中文/English 切换，登录后与后端同步（账号表 `locale` 列） |

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
3. 接口地址在 `src/utils/config.js` 的 `baseUrl`：
   - 开发者工具模拟器：`http://localhost:9091`
   - 真机调试：改为电脑的局域网 IP（如 `http://192.168.x.x:9091`），手机与电脑同一 Wi-Fi。
4. 数据库需已执行 `sql/add_theme_column.sql` 与 `sql/add_locale_column.sql` 迁移（见 Web 端仓库 `manager-vue3/sql/`）。

## 目录结构

```text
src/
+-- i18n/index.js               # 轻量 i18n（词条键与 Web 端一致）
+-- locales/                    # zh-CN / en-US 核心包 + pages/ 分组包
+-- stores/user.js              # Pinia 用户状态（uni storage 持久化）
+-- composables/
|   +-- useCrud.js              # 通用 CRUD（分页列表/表单/删除确认，与 Web 端语义对齐）
|   +-- useTheme.js             # 主题三档切换 + 系统深色跟随 + 后端同步
|   +-- useLocale.js            # 语言偏好 + 后端同步
+-- utils/request.js            # uni.request 封装（token 头、验证码 Cookie、401 统一处理）
+-- utils/config.js             # 后端地址
+-- styles/theme.scss           # CSS 变量主题 + 通用 xm-* 组件类
+-- pages/                      # 22 个页面（login 为入口，未登录自动跳转）
```

## 与 Web 端的交互适配说明

功能一致的前提下，交互形式按移动端习惯做了等价适配：

- 表格 → 卡片列表 + 底部弹层表单；分页 → 上拉/点击"加载更多"。
- 批量删除 → "管理"模式勾选后批量删除（管理员角色）。
- ECharts 图表 → 纯 CSS 占比条形统计（数据来源接口不变：`/attendance/getPie`、`/score/getLine`）。
- 日期控件 → 文本输入（后端按字符串存储）；附件选择 → `uni.chooseImage` + `uni.uploadFile`。

## 刻意保留中文的部分（与 Web 端一致）

数据库枚举值（考勤状态、请假状态、课程性质、上课时段等）按中文存储，界面仅翻译展示层文案；后端统计按中文值分组，两端行为一致。
