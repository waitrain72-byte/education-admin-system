# 数据库 ER 图

> 数据库：`xm_educational_manager`（MySQL 5.7，共 21 张表）
> 说明：系统采用**逻辑外键**设计（不建物理外键约束，由应用层保证一致性，便于批量导入与维护），下图为逻辑关系。
> 图表使用 Mermaid 渲染，可在 GitHub 直接查看，也可粘贴到 [mermaid.live](https://mermaid.live) 导出 PNG/SVG 插入论文。

## 一、核心业务 ER 图（16 张业务表）

```mermaid
erDiagram
    COLLEGE {
        int id PK "主键"
        varchar name "学院名称"
        varchar content "学院介绍"
    }
    SPECIALITY {
        int id PK "主键"
        varchar name "专业名称"
        varchar content "专业描述"
        int college_id FK "所属学院"
        int score "学分限定"
    }
    CLASSES {
        int id PK "主键"
        varchar name "班级名称"
        varchar content "班级描述"
        int teacher_id FK "班主任"
        int speciality_id FK "所属专业"
    }
    STUDENT {
        int id PK "主键"
        varchar username UK "登录账号"
        varchar password "密码(BCrypt)"
        varchar name "姓名"
        varchar avatar "头像"
        varchar role "角色"
        int college_id FK "学院"
        int speciality_id FK "专业"
        int class_id FK "班级"
        int score "已获学分"
        varchar theme "主题偏好"
        varchar locale "语言偏好"
    }
    TEACHER {
        int id PK "主键"
        varchar username UK "登录账号"
        varchar password "密码(BCrypt)"
        varchar name "姓名"
        varchar avatar "头像"
        varchar role "角色"
        varchar phone "电话"
        varchar email "邮箱"
        varchar title "职称"
        varchar theme "主题偏好"
        varchar locale "语言偏好"
    }
    ADMIN {
        int id PK "主键"
        varchar username UK "登录账号"
        varchar password "密码(BCrypt)"
        varchar name "姓名"
        varchar avatar "头像"
        varchar role "角色"
        varchar phone "电话"
        varchar email "邮箱"
        varchar theme "主题偏好"
        varchar locale "语言偏好"
    }
    COURSE {
        int id PK "主键"
        varchar name "课程名称"
        varchar type "课程类型(必修/选修)"
        int teacher_id FK "授课教师"
        int score "课程学分"
        int num "上课人数"
        varchar room "上课教室"
        varchar week "周几"
        varchar segment "第几大节"
        varchar status "上课状态"
    }
    CHOICE {
        int id PK "主键"
        int teacher_id FK "授课教师"
        int student_id FK "学生"
        int course_id FK "课程"
    }
    SCORE {
        int id PK "主键"
        int student_id FK "学生"
        int course_id FK "课程"
        int teacher_id FK "教师"
        double ordinary_score "平时分"
        double exam_score "考试分"
        double score "总成绩"
    }
    ATTENDANCE {
        int id PK "主键"
        int student_id FK "学生"
        int teacher_id FK "教师"
        int course_id FK "课程"
        varchar time "上课时间"
        varchar status "考勤状态(中文)"
    }
    HOMEWORK {
        int id PK "主键"
        varchar content "作业说明"
        int course_id FK "课程"
        int student_id FK "学生"
        int teacher_id FK "教师"
        varchar file "作业附件"
        varchar score "批改打分"
        varchar descr "批改说明"
    }
    APPLY {
        int id PK "主键"
        int student_id FK "学生"
        text content "请假说明"
        varchar time "请假时间"
        int day "请假天数"
        varchar status "审核状态(中文)"
        varchar descr "审核说明"
    }
    COMMENT {
        int id PK "主键"
        varchar name "课程名称"
        varchar teacher "授课教师姓名"
        varchar student "评教学生姓名"
        text content "评教内容"
        varchar time "评教时间"
    }
    NOTICE {
        int id PK "主键"
        varchar title "标题"
        varchar content "内容"
        varchar time "创建时间"
        varchar user "创建人"
    }
    EXAMPLAN {
        int id PK "主键"
        varchar name "标题"
        varchar content "内容"
        varchar time "发布时间"
    }
    ROOMPLAN {
        int id PK "主键"
        varchar name "教室名称"
        varchar status "教室状态"
        int num "容纳人数"
        varchar content "使用说明"
    }

    COLLEGE ||--o{ SPECIALITY : "开设专业"
    SPECIALITY ||--o{ CLASSES : "开设班级"
    CLASSES ||--o{ STUDENT : "包含学生"
    COLLEGE ||--o{ STUDENT : "归属学院"
    SPECIALITY ||--o{ STUDENT : "归属专业"
    TEACHER ||--o{ CLASSES : "担任班主任"
    TEACHER ||--o{ COURSE : "任教"
    STUDENT ||--o{ CHOICE : "选课"
    COURSE ||--o{ CHOICE : "被选"
    TEACHER ||--o{ CHOICE : "授课对应"
    STUDENT ||--o{ SCORE : "获得成绩"
    COURSE ||--o{ SCORE : "产生成绩"
    TEACHER ||--o{ SCORE : "录入成绩"
    STUDENT ||--o{ ATTENDANCE : "被考勤"
    COURSE ||--o{ ATTENDANCE : "课程考勤"
    TEACHER ||--o{ ATTENDANCE : "登记考勤"
    STUDENT ||--o{ HOMEWORK : "提交作业"
    COURSE ||--o{ HOMEWORK : "布置作业"
    TEACHER ||--o{ HOMEWORK : "批改作业"
    STUDENT ||--o{ APPLY : "提交请假"
    TEACHER ||--o{ COMMENT : "被评教(按姓名)"
    STUDENT ||--o{ COMMENT : "发起评教(按姓名)"
```

## 二、RBAC 权限与日志 ER 图（5 张系统表）

```mermaid
erDiagram
    SYS_ROLE {
        int id PK "主键"
        varchar code UK "角色标识(ADMIN/TEACHER/STUDENT，与账号表 role 一致)"
        varchar name "角色名称"
        varchar descr "角色说明"
    }
    SYS_PERMISSION {
        int id PK "主键"
        varchar code UK "权限码(模块:动作，如 score:manage)"
        varchar name "权限名称"
        varchar type "类型: menu=页面 button=操作"
        varchar module "所属模块"
        int sort_num "排序号"
    }
    SYS_ROLE_PERMISSION {
        int id PK "主键"
        int role_id FK "角色"
        int permission_id FK "权限"
    }
    SYS_LOGIN_LOG {
        int id PK "主键"
        varchar username "登录账号"
        varchar ip "登录IP"
        varchar status "状态(成功/失败)"
        varchar msg "说明"
        datetime create_time "时间"
    }
    SYS_OPER_LOG {
        int id PK "主键"
        varchar username "操作人账号"
        varchar module "操作模块(类#方法)"
        varchar type "请求方式"
        varchar url "请求地址"
        varchar params "请求参数(脱敏)"
        varchar ip "操作IP"
        varchar code "响应码"
        varchar msg "响应消息"
        int duration "耗时(毫秒)"
        datetime create_time "操作时间"
    }

    SYS_ROLE ||--o{ SYS_ROLE_PERMISSION : "拥有"
    SYS_PERMISSION ||--o{ SYS_ROLE_PERMISSION : "被授予"
```

> 账号三表（`admin` / `teacher` / `student`）的 `role` 字段与 `sys_role.code` **逻辑关联**（值同为 `ADMIN` / `TEACHER` / `STUDENT`），
> 登录时按该值查询角色对应的权限点集合，实现 RBAC 动态鉴权。`ADMIN` 角色在服务端切面直接放行。

## 三、设计要点说明

1. **以「课程」为中心的星型结构**：`choice`（选课）、`score`（成绩）、`attendance`（考勤）、`homework`（作业）四张表
   都是「学生 × 课程 × 教师」的关联实体——选课是前提，成绩/考勤/作业都发生在"某学生选了某教师的某门课"上。
2. **评教表按姓名弱关联**：`comment` 表存的是师生姓名而非 ID（历史设计），与师生表为弱关联，图中已标注。
3. **逻辑外键而非物理外键**：不建 `FOREIGN KEY` 约束，配合 `uk_username` 唯一索引、
   `idx_student_course` 等二级索引保证查询性能（索引设计见种子建表语句）。
4. **三账号表结构相近但分表存储**：`admin` / `teacher` / `student` 字段高度相似，分表是因为三角色的
   业务字段差异（学生有班级归属与学分，教师有职称）与数据隔离需求（各角色独立管理页）。
5. **独立实体**：`notice`（教务通知）、`examplan`（考试安排）、`roomplan`（教室安排）、`admin`（管理员）
   无外键关联，为全员公告/独立账号类数据；通知发布时通过 WebSocket 全员广播。
