-- ============================================================
-- 教务管理系统 演示种子数据（由净化后的数据库自动导出）
-- 演示账号（初始密码均为 123456）：管理员 admin；教师 luys、chenmin、liuyang、wangqiang；
--   学生 zhangsan、lisi、wangwu 等 15 人
-- 演示数据按 2026-2027 学年第一学期（2026-08-31 开学）编排：课程、选课、考勤、作业、成绩册、评价、公告、请假
-- 头像：files/ 目录仅保留 5 个在用文件，账号头像为 /api/files/ 相对路径；
--   课程资料的文件不入库，课程资料表、消息表与日志表仅结构
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

--
-- Table `admin`
--
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `role` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色标识',
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `theme` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'system' COMMENT '主题偏好: light/dark/system',
  `locale` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'zh-CN' COMMENT '界面语言: zh-CN/en-US',
  `theme_color` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '自定义主题色: #RRGGBB，空串表示用内置默认色',
  PRIMARY KEY (`id`) USING BTREE,
  -- 用户名唯一：既是登录查询的索引，也堵住「先查后插」的账号重复竞态
  UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='管理员';
INSERT INTO `admin` (`id`,`username`,`password`,`name`,`avatar`,`role`,`phone`,`email`,`theme`,`locale`,`theme_color`) VALUES (1,'admin','$2b$10$RL2AW18BBO.J2oje.TjxR.sABsocXXFTvC/nusjnJvyueZZxFSr5u','管理员','/api/files/7e2468d07dc47789c731faa6edbd11ea.jpg','ADMIN','12345678901','admin@xm.com','system','zh-CN','');
-- admin: 1 rows
-- >>> end-of-statement <<<

--
-- Table `apply`
--
DROP TABLE IF EXISTS `apply`;
CREATE TABLE `apply` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '请假说明',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请假时间',
  `day` int(10) DEFAULT NULL COMMENT '请假天数',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核状态',
  `descr` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='请假信息表';
INSERT INTO `apply` (`id`,`student_id`,`content`,`time`,`day`,`status`,`descr`) VALUES (1,1,'感冒发烧，需要去校医院就诊。','2026-09-22',1,'审核通过','注意休息，按时补交作业。'),(2,2,'家中有事需要回家处理。','2026-09-24',2,'审核通过','同意，返校后销假。'),(3,3,'参加全国大学生数学建模竞赛答辩。','2026-10-08',1,'待审核',NULL),(4,4,'参加校篮球队客场比赛。','2026-10-09',2,'待审核',NULL);
-- apply: 4 rows
-- >>> end-of-statement <<<

--
-- Table `assignment`
--
DROP TABLE IF EXISTS `assignment`;
CREATE TABLE `assignment` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `course_id` int(11) NOT NULL COMMENT '课程ID',
  `teacher_id` int(11) DEFAULT NULL COMMENT '布置的教师',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作业标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '作业要求',
  `attachment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件地址',
  `attachment_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件原文件名',
  `deadline` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '截止时间（yyyy-MM-dd HH:mm）',
  `full_score` int(11) NOT NULL DEFAULT '100' COMMENT '满分',
  `create_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '布置时间（yyyy-MM-dd HH:mm）',
  PRIMARY KEY (`id`),
  KEY `idx_assignment_course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='作业任务（老师布置，学生按任务提交）';
INSERT INTO `assignment` (`id`,`course_id`,`teacher_id`,`title`,`content`,`attachment`,`attachment_name`,`deadline`,`full_score`,`create_time`) VALUES (1,7,2,'作业一：线性表的两种实现','分别用顺序表和单链表实现线性表的插入、删除、查找，写一个 main 函数演示。\n提交源代码压缩包，或把关键代码直接贴在文本框里。',NULL,NULL,'2026-09-20 23:59',100,'2026-09-08 11:30'),(2,7,2,'作业二：栈的应用——表达式求值','用两个栈实现带括号的四则运算表达式求值，至少测试 5 个表达式（含错误输入）。',NULL,NULL,'2026-10-04 23:59',100,'2026-09-22 11:40'),(3,7,2,'作业三：二叉树的遍历','实现二叉树的先序、中序、后序（递归与非递归各一种）和层序遍历，输出遍历序列。',NULL,NULL,'2026-10-15 23:59',100,'2026-09-29 11:35'),(4,8,2,'实验一：面向对象基础','设计「学生」「课程」两个类，实现选课与成绩统计，要求用到封装与构造方法重载。',NULL,NULL,'2026-09-27 23:59',20,'2026-09-15 15:50'),(5,8,2,'实验二：集合框架通讯录','用 HashMap 实现一个命令行通讯录：增删改查、按姓名排序输出、保存到文件。',NULL,NULL,'2026-10-12 23:59',20,'2026-09-29 15:45'),(6,1,2,'习题 2.3（导数的计算）','教材习题 2.3 第 1、3、5、8、12 题，拍照或扫描成 PDF 提交。',NULL,NULL,'2026-10-11 23:59',100,'2026-09-28 09:50');
-- assignment: 6 rows
-- >>> end-of-statement <<<

--
-- Table `attendance`
--
DROP TABLE IF EXISTS `attendance`;
CREATE TABLE `attendance` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上课时间',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考勤状态',
  `session_id` int(11) DEFAULT NULL COMMENT '签到场次（老师手工登记为空）',
  PRIMARY KEY (`id`) USING BTREE,
  -- 联合索引对应 AttendanceMapper.selectByStudentIdAndCourseIdAndTime 的重复录入校验
  KEY `idx_att_student_course` (`student_id`,`course_id`),
  KEY `idx_att_course` (`course_id`),
  KEY `idx_att_teacher` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=473 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='考勤信息表';
INSERT INTO `attendance` (`id`,`student_id`,`teacher_id`,`course_id`,`time`,`status`,`session_id`) VALUES (92,11,2,1,'2026-08-31','正常',NULL),(93,12,2,1,'2026-08-31','正常',NULL),(94,10,2,1,'2026-08-31','正常',NULL),(95,14,2,1,'2026-08-31','正常',NULL),(96,2,2,1,'2026-08-31','正常',NULL),(97,5,2,1,'2026-08-31','正常',NULL),(98,15,2,1,'2026-08-31','正常',NULL),(99,6,2,1,'2026-08-31','正常',NULL),(100,3,2,1,'2026-08-31','正常',NULL),(101,13,2,1,'2026-08-31','正常',NULL),(102,8,2,1,'2026-08-31','正常',NULL),(103,1,2,1,'2026-08-31','正常',NULL),(104,4,2,1,'2026-08-31','正常',NULL),(105,9,2,1,'2026-08-31','正常',NULL),(106,7,2,1,'2026-08-31','正常',NULL),(107,11,2,1,'2026-09-07','正常',NULL),(108,12,2,1,'2026-09-07','正常',NULL),(109,10,2,1,'2026-09-07','正常',NULL),(110,14,2,1,'2026-09-07','正常',NULL),(111,2,2,1,'2026-09-07','正常',NULL),(112,5,2,1,'2026-09-07','正常',NULL),(113,15,2,1,'2026-09-07','正常',NULL),(114,6,2,1,'2026-09-07','正常',NULL),(115,3,2,1,'2026-09-07','正常',NULL),(116,13,2,1,'2026-09-07','正常',NULL),(117,8,2,1,'2026-09-07','正常',NULL),(118,1,2,1,'2026-09-07','正常',NULL),(119,4,2,1,'2026-09-07','正常',NULL),(120,9,2,1,'2026-09-07','正常',NULL),(121,7,2,1,'2026-09-07','正常',NULL),(122,11,2,1,'2026-09-14','正常',NULL),(123,12,2,1,'2026-09-14','正常',NULL),(124,10,2,1,'2026-09-14','正常',NULL),(125,14,2,1,'2026-09-14','正常',NULL),(126,2,2,1,'2026-09-14','缺勤',NULL),(127,5,2,1,'2026-09-14','正常',NULL),(128,15,2,1,'2026-09-14','正常',NULL),(129,6,2,1,'2026-09-14','正常',NULL),(130,3,2,1,'2026-09-14','正常',NULL),(131,13,2,1,'2026-09-14','正常',NULL),(132,8,2,1,'2026-09-14','正常',NULL),(133,1,2,1,'2026-09-14','正常',NULL),(134,4,2,1,'2026-09-14','正常',NULL),(135,9,2,1,'2026-09-14','正常',NULL),(136,7,2,1,'2026-09-14','正常',NULL),(137,11,2,1,'2026-09-21','正常',NULL),(138,12,2,1,'2026-09-21','正常',NULL),(139,10,2,1,'2026-09-21','正常',NULL),(140,14,2,1,'2026-09-21','正常',NULL),(141,2,2,1,'2026-09-21','缺勤',NULL),(142,5,2,1,'2026-09-21','正常',NULL),(143,15,2,1,'2026-09-21','正常',NULL),(144,6,2,1,'2026-09-21','正常',NULL),(145,3,2,1,'2026-09-21','正常',NULL),(146,13,2,1,'2026-09-21','正常',NULL),(147,8,2,1,'2026-09-21','正常',NULL),(148,1,2,1,'2026-09-21','正常',NULL),(149,4,2,1,'2026-09-21','正常',NULL),(150,9,2,1,'2026-09-21','正常',NULL),(151,7,2,1,'2026-09-21','正常',NULL),(152,11,2,1,'2026-09-28','正常',NULL),(153,12,2,1,'2026-09-28','正常',NULL),(154,10,2,1,'2026-09-28','正常',NULL),(155,14,2,1,'2026-09-28','正常',NULL),(156,2,2,1,'2026-09-28','正常',NULL),(157,5,2,1,'2026-09-28','正常',NULL),(158,15,2,1,'2026-09-28','正常',NULL),(159,6,2,1,'2026-09-28','正常',NULL),(160,3,2,1,'2026-09-28','正常',NULL),(161,13,2,1,'2026-09-28','正常',NULL),(162,8,2,1,'2026-09-28','迟到',NULL),(163,1,2,1,'2026-09-28','正常',NULL),(164,4,2,1,'2026-09-28','正常',NULL),(165,9,2,1,'2026-09-28','正常',NULL),(166,7,2,1,'2026-09-28','正常',NULL),(219,11,2,7,'2026-09-01','正常',NULL),(220,11,2,7,'2026-09-08','正常',NULL),(221,11,2,7,'2026-09-15','正常',NULL),(222,11,2,7,'2026-09-22','正常',NULL),(223,11,2,7,'2026-09-29','正常',NULL),(224,11,2,8,'2026-09-01','正常',NULL),(225,11,2,8,'2026-09-08','正常',NULL),(226,11,2,8,'2026-09-15','正常',NULL),(227,11,2,8,'2026-09-22','正常',NULL),(228,11,2,8,'2026-09-29','正常',NULL),(229,12,2,7,'2026-09-01','正常',NULL),(230,12,2,7,'2026-09-08','正常',NULL),(231,12,2,7,'2026-09-15','正常',NULL),(232,12,2,7,'2026-09-22','正常',NULL),(233,12,2,7,'2026-09-29','正常',NULL),(234,10,2,7,'2026-09-01','正常',NULL),(235,10,2,7,'2026-09-08','正常',NULL),(236,10,2,7,'2026-09-15','正常',NULL),(237,10,2,7,'2026-09-22','正常',NULL),(238,10,2,7,'2026-09-29','正常',NULL),(239,10,2,8,'2026-09-01','正常',NULL),(240,10,2,8,'2026-09-08','正常',NULL),(241,10,2,8,'2026-09-15','正常',NULL),(242,10,2,8,'2026-09-22','正常',NULL),(243,10,2,8,'2026-09-29','正常',NULL),(244,14,2,7,'2026-09-01','正常',NULL),(245,14,2,7,'2026-09-08','正常',NULL),(246,14,2,7,'2026-09-15','正常',NULL),(247,14,2,7,'2026-09-22','正常',NULL),(248,14,2,7,'2026-09-29','正常',NULL),(249,2,2,7,'2026-09-01','正常',NULL),(250,2,2,7,'2026-09-08','正常',NULL),(251,2,2,7,'2026-09-15','正常',NULL),(252,2,2,7,'2026-09-22','正常',NULL),(253,2,2,7,'2026-09-29','正常',NULL),(254,5,2,7,'2026-09-01','正常',NULL),(255,5,2,7,'2026-09-08','正常',NULL),(256,5,2,7,'2026-09-15','正常',NULL),(257,5,2,7,'2026-09-22','正常',NULL),(258,5,2,7,'2026-09-29','正常',NULL),(259,5,2,8,'2026-09-01','正常',NULL),(260,5,2,8,'2026-09-08','正常',NULL),(261,5,2,8,'2026-09-15','正常',NULL),(262,5,2,8,'2026-09-22','正常',NULL),(263,5,2,8,'2026-09-29','正常',NULL),(264,15,2,7,'2026-09-01','正常',NULL),(265,15,2,7,'2026-09-08','正常',NULL),(266,15,2,7,'2026-09-15','正常',NULL),(267,15,2,7,'2026-09-22','正常',NULL),(268,15,2,7,'2026-09-29','正常',NULL),(269,6,2,7,'2026-09-01','正常',NULL),(270,6,2,7,'2026-09-08','正常',NULL),(271,6,2,7,'2026-09-15','正常',NULL),(272,6,2,7,'2026-09-22','正常',NULL),(273,6,2,7,'2026-09-29','正常',NULL),(274,6,2,8,'2026-09-01','正常',NULL),(275,6,2,8,'2026-09-08','正常',NULL),(276,6,2,8,'2026-09-15','正常',NULL),(277,6,2,8,'2026-09-22','正常',NULL),(278,6,2,8,'2026-09-29','正常',NULL),(279,3,2,7,'2026-09-01','正常',NULL),(280,3,2,7,'2026-09-08','正常',NULL),(281,3,2,7,'2026-09-15','正常',NULL),(282,3,2,7,'2026-09-22','正常',NULL),(283,3,2,7,'2026-09-29','正常',NULL),(284,3,2,8,'2026-09-01','正常',NULL),(285,3,2,8,'2026-09-08','正常',NULL),(286,3,2,8,'2026-09-15','正常',NULL),(287,3,2,8,'2026-09-22','正常',NULL),(288,3,2,8,'2026-09-29','正常',NULL),(289,13,2,7,'2026-09-01','正常',NULL),(290,13,2,7,'2026-09-08','正常',NULL),(291,13,2,7,'2026-09-15','正常',NULL),(292,13,2,7,'2026-09-22','正常',NULL),(293,13,2,7,'2026-09-29','正常',NULL),(294,8,2,7,'2026-09-01','正常',NULL),(295,8,2,7,'2026-09-08','缺勤',NULL),(296,8,2,7,'2026-09-15','正常',NULL),(297,8,2,7,'2026-09-22','缺勤',NULL),(298,8,2,7,'2026-09-29','缺勤',NULL),(299,8,2,8,'2026-09-01','正常',NULL),(300,8,2,8,'2026-09-08','缺勤',NULL),(301,8,2,8,'2026-09-15','正常',NULL),(302,8,2,8,'2026-09-22','缺勤',NULL),(303,8,2,8,'2026-09-29','缺勤',NULL),(304,1,2,7,'2026-09-01','正常',NULL),(305,1,2,7,'2026-09-08','正常',NULL),(306,1,2,7,'2026-09-15','迟到',NULL),(307,1,2,7,'2026-09-22','请假',NULL),(308,1,2,7,'2026-09-29','正常',NULL),(309,1,2,8,'2026-09-01','正常',NULL),(310,1,2,8,'2026-09-08','正常',NULL),(311,1,2,8,'2026-09-15','正常',NULL),(312,1,2,8,'2026-09-22','请假',NULL),(313,1,2,8,'2026-09-29','正常',NULL),(314,4,2,7,'2026-09-01','正常',NULL),(315,4,2,7,'2026-09-08','正常',NULL),(316,4,2,7,'2026-09-15','正常',NULL),(317,4,2,7,'2026-09-22','正常',NULL),(318,4,2,7,'2026-09-29','正常',NULL),(319,4,2,8,'2026-09-01','正常',NULL),(320,4,2,8,'2026-09-08','正常',NULL),(321,4,2,8,'2026-09-15','正常',NULL),(322,4,2,8,'2026-09-22','正常',NULL),(323,4,2,8,'2026-09-29','正常',NULL),(324,9,2,7,'2026-09-01','正常',NULL),(325,9,2,7,'2026-09-08','正常',NULL),(326,9,2,7,'2026-09-15','正常',NULL),(327,9,2,7,'2026-09-22','正常',NULL),(328,9,2,7,'2026-09-29','正常',NULL),(329,9,2,8,'2026-09-01','正常',NULL),(330,9,2,8,'2026-09-08','正常',NULL),(331,9,2,8,'2026-09-15','正常',NULL),(332,9,2,8,'2026-09-22','正常',NULL),(333,9,2,8,'2026-09-29','正常',NULL),(334,7,2,7,'2026-09-01','正常',NULL),(335,7,2,7,'2026-09-08','正常',NULL),(336,7,2,7,'2026-09-15','正常',NULL),(337,7,2,7,'2026-09-22','正常',NULL),(338,7,2,7,'2026-09-29','正常',NULL),(339,7,2,8,'2026-09-01','正常',NULL),(340,7,2,8,'2026-09-08','正常',NULL),(341,7,2,8,'2026-09-15','正常',NULL),(342,7,2,8,'2026-09-22','正常',NULL),(343,7,2,8,'2026-09-29','正常',NULL),(346,11,3,9,'2026-09-02','正常',NULL),(347,12,3,9,'2026-09-02','正常',NULL),(348,10,3,9,'2026-09-02','正常',NULL),(349,14,3,9,'2026-09-02','正常',NULL),(350,5,3,9,'2026-09-02','正常',NULL),(351,15,3,9,'2026-09-02','正常',NULL),(352,6,3,9,'2026-09-02','正常',NULL),(353,3,3,9,'2026-09-02','正常',NULL),(354,13,3,9,'2026-09-02','正常',NULL),(355,8,3,9,'2026-09-02','正常',NULL),(356,1,3,9,'2026-09-02','正常',NULL),(357,4,3,9,'2026-09-02','正常',NULL),(358,9,3,9,'2026-09-02','正常',NULL),(359,7,3,9,'2026-09-02','正常',NULL),(360,11,3,9,'2026-09-09','正常',NULL),(361,12,3,9,'2026-09-09','正常',NULL),(362,10,3,9,'2026-09-09','正常',NULL),(363,14,3,9,'2026-09-09','正常',NULL),(364,5,3,9,'2026-09-09','正常',NULL),(365,15,3,9,'2026-09-09','正常',NULL),(366,6,3,9,'2026-09-09','正常',NULL),(367,3,3,9,'2026-09-09','正常',NULL),(368,13,3,9,'2026-09-09','正常',NULL),(369,8,3,9,'2026-09-09','正常',NULL),(370,1,3,9,'2026-09-09','正常',NULL),(371,4,3,9,'2026-09-09','正常',NULL),(372,9,3,9,'2026-09-09','正常',NULL),(373,7,3,9,'2026-09-09','正常',NULL),(374,11,3,9,'2026-09-16','正常',NULL),(375,12,3,9,'2026-09-16','正常',NULL),(376,10,3,9,'2026-09-16','正常',NULL),(377,14,3,9,'2026-09-16','正常',NULL),(378,5,3,9,'2026-09-16','正常',NULL),(379,15,3,9,'2026-09-16','正常',NULL),(380,6,3,9,'2026-09-16','正常',NULL),(381,3,3,9,'2026-09-16','正常',NULL),(382,13,3,9,'2026-09-16','正常',NULL),(383,8,3,9,'2026-09-16','正常',NULL),(384,1,3,9,'2026-09-16','正常',NULL),(385,4,3,9,'2026-09-16','正常',NULL),(386,9,3,9,'2026-09-16','正常',NULL),(387,7,3,9,'2026-09-16','正常',NULL),(388,11,3,9,'2026-09-23','正常',NULL),(389,12,3,9,'2026-09-23','正常',NULL),(390,10,3,9,'2026-09-23','正常',NULL),(391,14,3,9,'2026-09-23','正常',NULL),(392,5,3,9,'2026-09-23','正常',NULL),(393,15,3,9,'2026-09-23','正常',NULL),(394,6,3,9,'2026-09-23','早退',NULL),(395,3,3,9,'2026-09-23','正常',NULL),(396,13,3,9,'2026-09-23','正常',NULL),(397,8,3,9,'2026-09-23','正常',NULL),(398,1,3,9,'2026-09-23','正常',NULL),(399,4,3,9,'2026-09-23','正常',NULL),(400,9,3,9,'2026-09-23','正常',NULL),(401,7,3,9,'2026-09-23','正常',NULL),(402,11,3,9,'2026-09-30','正常',NULL),(403,12,3,9,'2026-09-30','正常',NULL),(404,10,3,9,'2026-09-30','正常',NULL),(405,14,3,9,'2026-09-30','正常',NULL),(406,5,3,9,'2026-09-30','正常',NULL),(407,15,3,9,'2026-09-30','正常',NULL),(408,6,3,9,'2026-09-30','正常',NULL),(409,3,3,9,'2026-09-30','正常',NULL),(410,13,3,9,'2026-09-30','正常',NULL),(411,8,3,9,'2026-09-30','正常',NULL),(412,1,3,9,'2026-09-30','正常',NULL),(413,4,3,9,'2026-09-30','正常',NULL),(414,9,3,9,'2026-09-30','正常',NULL),(415,7,3,9,'2026-09-30','正常',NULL);
-- attendance: 270 rows
-- >>> end-of-statement <<<

--
-- Table `attendance_session`
--
DROP TABLE IF EXISTS `attendance_session`;
CREATE TABLE `attendance_session` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `course_id` int(11) NOT NULL COMMENT '课程ID',
  `teacher_id` int(11) DEFAULT NULL COMMENT '发起的教师',
  `code` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '签到码',
  `date` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上课日期（yyyy-MM-dd，写入考勤记录的时间）',
  `start_time` varchar(19) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发起时间（yyyy-MM-dd HH:mm:ss）',
  `expire_time` varchar(19) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '截止时间（yyyy-MM-dd HH:mm:ss）',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '进行中' COMMENT '进行中 / 已结束',
  PRIMARY KEY (`id`),
  KEY `idx_session_course` (`course_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='课堂签到场次';
-- attendance_session: 0 rows
-- >>> end-of-statement <<<

--
-- Table `choice`
--
DROP TABLE IF EXISTS `choice`;
CREATE TABLE `choice` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '授课教师',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_choice_student` (`student_id`),
  KEY `idx_choice_course` (`course_id`),
  KEY `idx_choice_teacher` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=135 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='选课信息表';
INSERT INTO `choice` (`id`,`teacher_id`,`student_id`,`course_id`) VALUES (8,2,11,1),(9,2,11,7),(10,2,11,8),(11,3,11,9),(12,4,11,10),(13,2,11,14),(14,4,11,15),(15,2,12,1),(16,2,12,7),(17,3,12,9),(18,4,12,10),(19,5,12,11),(20,4,12,15),(21,2,10,1),(22,2,10,7),(23,2,10,8),(24,3,10,9),(25,4,10,10),(26,2,10,14),(27,4,10,15),(28,2,14,1),(29,2,14,7),(30,3,14,9),(31,4,14,10),(32,5,14,11),(33,4,14,15),(34,2,2,1),(35,2,2,5),(36,2,2,6),(37,2,2,7),(38,4,2,10),(39,2,2,14),(40,2,5,1),(41,2,5,7),(42,2,5,8),(43,3,5,9),(44,4,5,10),(45,5,5,11),(46,2,5,14),(47,4,5,15),(48,2,15,1),(49,2,15,7),(50,3,15,9),(51,4,15,10),(52,5,15,11),(53,4,15,15),(54,2,6,1),(55,2,6,7),(56,2,6,8),(57,3,6,9),(58,4,6,10),(59,5,6,11),(60,2,6,14),(61,4,6,15),(62,2,3,1),(63,2,3,6),(64,2,3,7),(65,2,3,8),(66,3,3,9),(67,4,3,15),(68,2,13,1),(69,2,13,7),(70,3,13,9),(71,4,13,10),(72,5,13,11),(73,4,13,15),(74,2,8,1),(75,2,8,7),(76,2,8,8),(77,3,8,9),(78,4,8,10),(79,2,8,14),(80,4,8,15),(81,2,1,1),(82,2,1,5),(83,2,1,6),(84,2,1,7),(85,2,1,8),(86,3,1,9),(87,4,1,10),(88,5,1,11),(89,2,1,14),(90,4,1,15),(91,2,4,1),(92,2,4,7),(93,2,4,8),(94,3,4,9),(95,4,4,10),(96,5,4,11),(97,2,4,14),(98,4,4,15),(99,2,9,1),(100,2,9,7),(101,2,9,8),(102,3,9,9),(103,4,9,10),(104,2,9,14),(105,4,9,15),(106,2,7,1),(107,2,7,7),(108,2,7,8),(109,3,7,9),(110,4,7,10),(111,2,7,14),(112,4,7,15);
-- choice: 105 rows
-- >>> end-of-statement <<<

--
-- Table `classes`
--
DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级名称',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级描述',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `speciality_id` int(10) DEFAULT NULL COMMENT '专业ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='班级信息表';
INSERT INTO `classes` (`id`,`name`,`content`,`teacher_id`,`speciality_id`) VALUES (1,'材控1班','巴拉巴拉巴拉-------',2,3),(2,'物联网1班','巴拉巴拉巴拉-------',2,1),(3,'马克思1班','巴拉巴拉巴拉-------',2,4),(4,'电子工程1班','巴拉巴拉巴拉-------',2,5),(5,'计科1班','	\n巴拉巴拉巴拉-------',2,6);
-- classes: 5 rows
-- >>> end-of-statement <<<

--
-- Table `college`
--
DROP TABLE IF EXISTS `college`;
CREATE TABLE `college` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学院名称',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学院介绍',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='学院信息表';
INSERT INTO `college` (`id`,`name`,`content`) VALUES (1,'信息工程学院','巴拉巴拉巴拉-------'),(2,'软件学院','巴拉巴拉巴拉-------'),(3,'计算机与物联网学院','巴拉巴拉巴拉-------'),(4,'马克思主义学院','巴拉巴拉巴拉-------'),(5,'材料工程学院','巴拉巴拉巴拉-------');
-- college: 5 rows
-- >>> end-of-statement <<<

--
-- Table `comment`
--
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程名称',
  `teacher` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '授课教师',
  `student` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评教学生',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '评教内容',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评教时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='评教信息表';
INSERT INTO `comment` (`id`,`name`,`teacher`,`student`,`content`,`time`) VALUES (11,'中国近代史纲要','路易斯','张三','史料讲得很扎实，课堂讨论很有启发。','2026-01-10 09:15:00'),(12,'C 语言程序设计','路易斯','张三','上机练习安排得好，指针一章讲得透彻。','2026-01-12 20:03:00');
-- comment: 2 rows
-- >>> end-of-statement <<<

--
-- Table `course`
--
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程名称',
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程类型',
  `teacher_id` int(10) DEFAULT NULL COMMENT '授课教师',
  `score` int(10) DEFAULT NULL COMMENT '课程学分',
  `num` int(10) DEFAULT NULL COMMENT '上课人数',
  `room` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上课教室',
  `week` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '周几',
  `segment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第几大节',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上课状态',
  `weight_attendance` int(11) NOT NULL DEFAULT '0' COMMENT '总评权重：考勤（%）',
  `weight_homework` int(11) NOT NULL DEFAULT '0' COMMENT '总评权重：作业（%）',
  `weight_ordinary` int(11) NOT NULL DEFAULT '30' COMMENT '总评权重：平时（%）',
  `weight_exam` int(11) NOT NULL DEFAULT '70' COMMENT '总评权重：期末（%）',
  `intro` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程简介',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_room_week_segment` (`room`,`week`,`segment`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='课程信息表';
INSERT INTO `course` (`id`,`name`,`type`,`teacher_id`,`score`,`num`,`room`,`week`,`segment`,`status`,`weight_attendance`,`weight_homework`,`weight_ordinary`,`weight_exam`,`intro`) VALUES (1,'高等数学','必修',2,5,120,'7701','星期一','第一大节（08:30 ~ 10:10）','已开课',0,0,30,70,'一元函数微积分：极限与连续、导数与微分、中值定理、不定积分与定积分。\n考核：平时作业与课堂表现 30%，期末闭卷考试 70%。'),(5,'线性代数','必修',2,3,80,'7705','星期五','第三大节（14:00 ~ 15:40）','已开课',0,0,30,70,NULL),(6,'中国近代史纲要','选修',2,2,50,'7706','星期五','第三大节（14:00 ~ 15:40）','已结课',0,0,30,70,NULL),(7,'数据结构','必修',2,3,60,'7710','星期二','第二大节（10:30 ~ 12:10）','已开课',10,20,20,50,'线性表、栈与队列、树与二叉树、图、查找与排序。课堂讲原理，作业用 C/Java 实现。\n考核：考勤 10%、作业 20%、课堂测验 20%、期末 50%。'),(8,'Java 程序设计','必修',2,3,40,'7711','星期二','第三大节（14:00 ~ 15:40）','已开课',10,30,10,50,'面向对象编程入门：类与对象、封装继承多态、集合框架、异常与 IO、简单的图形界面。每两周一次上机实验。\n考核：考勤 10%、实验 30%、课堂表现 10%、期末上机考试 50%。'),(9,'离散数学','必修',3,3,60,'7708','星期三','第一大节（08:30 ~ 10:10）','已开课',0,0,30,70,'命题逻辑、谓词逻辑、集合与关系、图论基础。'),(10,'大学英语','必修',4,2,50,'7707','星期一','第三大节（14:00 ~ 15:40）','已开课',0,0,30,70,NULL),(11,'体育（篮球）','选修',5,1,40,'TY01','星期四','第四大节（16:00 ~ 17:40）','已开课',0,0,30,70,NULL),(12,'Python 数据分析','选修',3,2,40,'7710','星期四','第五大节（19:00 ~ 20:40）','未开课',0,0,30,70,NULL),(13,'操作系统','必修',4,3,60,NULL,NULL,NULL,'未开课',0,0,30,70,NULL),(14,'C 语言程序设计','必修',2,4,60,'7711','星期三','第二大节（10:30 ~ 12:10）','已结课',0,0,30,70,'C 语言基础：数据类型、控制结构、函数、数组与指针、结构体、文件。'),(15,'大学物理','必修',4,4,60,'7705','星期四','第一大节（08:30 ~ 10:10）','已结课',0,0,30,70,NULL);
-- course: 12 rows
-- >>> end-of-statement <<<

--
-- Table `course_eval`
--
DROP TABLE IF EXISTS `course_eval`;
CREATE TABLE `course_eval` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `course_id` int(11) NOT NULL COMMENT '课程ID',
  `teacher_id` int(11) DEFAULT NULL COMMENT '任课教师',
  `student_id` int(11) NOT NULL COMMENT '评价的学生（对教师匿名展示）',
  `attitude` tinyint(4) NOT NULL COMMENT '教学态度 1-5',
  `content_score` tinyint(4) NOT NULL COMMENT '教学内容 1-5',
  `method` tinyint(4) NOT NULL COMMENT '教学方法 1-5',
  `effect` tinyint(4) NOT NULL COMMENT '教学效果 1-5',
  `support` tinyint(4) NOT NULL COMMENT '课后辅导 1-5',
  `comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文字评价',
  `create_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评价时间（yyyy-MM-dd HH:mm）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_course_student` (`course_id`,`student_id`),
  KEY `idx_eval_teacher` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='课程评价（五个维度打分 + 文字）';
INSERT INTO `course_eval` (`id`,`course_id`,`teacher_id`,`student_id`,`attitude`,`content_score`,`method`,`effect`,`support`,`comment`,`create_time`) VALUES (1,14,2,4,5,5,4,5,4,'指针那一章讲得很透，上机题目难度合适。','2026-01-08 20:12'),(2,14,2,5,5,4,4,4,5,'老师课后答疑很耐心。','2026-01-08 21:30'),(3,14,2,6,4,4,3,4,4,NULL,'2026-01-09 09:05'),(4,14,2,7,5,5,5,5,5,'最喜欢的一门专业课！','2026-01-09 10:40'),(5,14,2,9,4,3,3,3,4,'进度有点快，希望多讲几道例题。','2026-01-09 13:22'),(6,14,2,10,5,4,5,4,4,NULL,'2026-01-10 16:48'),(7,15,4,4,4,4,4,4,3,'实验课收获很大。','2026-01-11 19:00'),(8,15,4,5,5,5,4,5,4,NULL,'2026-01-11 20:31'),(9,6,2,2,5,5,5,4,4,'课堂讨论很有意思。','2026-01-07 18:45');
-- course_eval: 9 rows
-- >>> end-of-statement <<<

--
-- Table `course_resource`
--
DROP TABLE IF EXISTS `course_resource`;
CREATE TABLE `course_resource` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `course_id` int(11) NOT NULL COMMENT '课程ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资料名称（原文件名）',
  `file` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件地址',
  `size` bigint(20) DEFAULT NULL COMMENT '字节数',
  `uploader_id` int(11) DEFAULT NULL COMMENT '上传人ID',
  `uploader_role` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上传人角色',
  `create_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上传时间（yyyy-MM-dd HH:mm）',
  PRIMARY KEY (`id`),
  KEY `idx_resource_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='课程资料';
-- course_resource: 0 rows
-- >>> end-of-statement <<<

--
-- Table `examplan`
--
DROP TABLE IF EXISTS `examplan`;
CREATE TABLE `examplan` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布时间',
  `exam_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考试时间（yyyy-MM-dd HH:mm）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='考试安排表';
INSERT INTO `examplan` (`id`,`name`,`content`,`time`,`exam_time`) VALUES (3,'2026 年秋季期中考试安排','期中考试安排在第 9 周进行，各科考场与时间以教务通知为准。','2026-08-26 10:00:00','2026-11-02 09:00'),(4,'全国计算机等级考试','9 月全国计算机等级考试，准考证请提前打印。','2026-08-28 09:00:00','2026-09-26 08:30'),(5,'数据结构 期中测验','第 8 周周三下午，地点 7710，闭卷，可带一张 A4 手写提纲。','2026-10-05 16:20:00','2026-10-21 14:00'),(6,'大学英语四级考试','全国大学英语四级考试，请按准考证上的考场与时间参加。','2026-10-05 16:25:00','2026-12-12 09:00');
-- examplan: 4 rows
-- >>> end-of-statement <<<

--
-- Table `homework`
--
DROP TABLE IF EXISTS `homework`;
CREATE TABLE `homework` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `assignment_id` int(11) DEFAULT NULL COMMENT '所属作业任务（旧版自由提交为空）',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程说明',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `file` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作业文件',
  `score` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打分',
  `descr` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  `submit_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提交时间（yyyy-MM-dd HH:mm）',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '已提交 / 已批改',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件原文件名',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_hw_student` (`student_id`),
  KEY `idx_hw_course` (`course_id`),
  KEY `idx_hw_teacher` (`teacher_id`),
  KEY `idx_homework_assignment` (`assignment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='作业信息表';
INSERT INTO `homework` (`id`,`assignment_id`,`content`,`course_id`,`student_id`,`teacher_id`,`file`,`score`,`descr`,`submit_time`,`status`,`file_name`) VALUES (48,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,11,2,NULL,'91','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(49,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,12,2,NULL,'70','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(50,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,10,2,NULL,'84','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(51,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,14,2,NULL,'84','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(52,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,2,2,NULL,'84','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(53,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,5,2,NULL,'77','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(54,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,6,2,NULL,'84','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(55,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,3,2,NULL,'91','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(56,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,13,2,NULL,'77','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(57,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,1,2,NULL,'77','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(58,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,4,2,NULL,'70','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(59,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,9,2,NULL,'77','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(60,1,'顺序表与单链表的实现见代码，main 中演示了插入、删除和查找。',7,7,2,NULL,'91','实现完整，注意边界条件。','2026-09-19 21:30','已批改',NULL),(63,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,11,2,NULL,NULL,NULL,'2026-10-03 22:10','已提交',NULL),(64,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,10,2,NULL,NULL,NULL,'2026-10-03 22:10','已提交',NULL),(65,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,2,2,NULL,'87','思路清晰，测试用例可以再多一些。','2026-10-03 22:10','已批改',NULL),(66,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,5,2,NULL,'87','思路清晰，测试用例可以再多一些。','2026-10-03 22:10','已批改',NULL),(67,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,15,2,NULL,NULL,NULL,'2026-10-03 22:10','已提交',NULL),(68,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,6,2,NULL,'65','思路清晰，测试用例可以再多一些。','2026-10-03 22:10','已批改',NULL),(69,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,3,2,NULL,'65','思路清晰，测试用例可以再多一些。','2026-10-03 22:10','已批改',NULL),(70,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,13,2,NULL,NULL,NULL,'2026-10-03 22:10','已提交',NULL),(71,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,1,2,NULL,'76','思路清晰，测试用例可以再多一些。','2026-10-03 22:10','已批改',NULL),(72,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,4,2,NULL,'76','思路清晰，测试用例可以再多一些。','2026-10-03 22:10','已批改',NULL),(73,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,9,2,NULL,'65','思路清晰，测试用例可以再多一些。','2026-10-03 22:10','已批改',NULL),(74,2,'用操作数栈和运算符栈实现，支持括号与优先级，错误输入会给出提示。',7,7,2,NULL,'76','思路清晰，测试用例可以再多一些。','2026-10-03 22:10','已批改',NULL),(78,3,'递归与非递归遍历都实现了，层序遍历用队列。',7,5,2,NULL,NULL,NULL,'2026-10-07 20:15','已提交',NULL),(79,3,'递归与非递归遍历都实现了，层序遍历用队列。',7,3,2,NULL,NULL,NULL,'2026-10-07 20:15','已提交',NULL),(80,3,'递归与非递归遍历都实现了，层序遍历用队列。',7,13,2,NULL,NULL,NULL,'2026-10-07 20:15','已提交',NULL),(81,3,'递归与非递归遍历都实现了，层序遍历用队列。',7,4,2,NULL,NULL,NULL,'2026-10-07 20:15','已提交',NULL),(82,3,'递归与非递归遍历都实现了，层序遍历用队列。',7,9,2,NULL,NULL,NULL,'2026-10-07 20:15','已提交',NULL),(85,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,11,2,NULL,'19','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(86,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,10,2,NULL,'16','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(87,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,5,2,NULL,'15','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(88,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,6,2,NULL,'18','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(89,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,3,2,NULL,'16','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(90,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,8,2,NULL,'17','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(91,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,1,2,NULL,'17','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(92,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,4,2,NULL,'19','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(93,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,9,2,NULL,'20','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(94,4,'类图与代码见附件说明，实现了选课和平均分统计。',8,7,2,NULL,'14','结构清楚，构造方法重载用得好。','2026-09-26 19:40','已批改',NULL),(100,5,'通讯录支持增删改查和按姓名排序，退出时保存到 contacts.txt。',8,10,2,NULL,NULL,NULL,'2026-10-07 21:05','已提交',NULL),(101,5,'通讯录支持增删改查和按姓名排序，退出时保存到 contacts.txt。',8,5,2,NULL,NULL,NULL,'2026-10-07 21:05','已提交',NULL),(102,5,'通讯录支持增删改查和按姓名排序，退出时保存到 contacts.txt。',8,6,2,NULL,NULL,NULL,'2026-10-07 21:05','已提交',NULL),(103,5,'通讯录支持增删改查和按姓名排序，退出时保存到 contacts.txt。',8,3,2,NULL,NULL,NULL,'2026-10-07 21:05','已提交',NULL),(104,5,'通讯录支持增删改查和按姓名排序，退出时保存到 contacts.txt。',8,4,2,NULL,NULL,NULL,'2026-10-07 21:05','已提交',NULL),(105,5,'通讯录支持增删改查和按姓名排序，退出时保存到 contacts.txt。',8,9,2,NULL,NULL,NULL,'2026-10-07 21:05','已提交',NULL),(107,NULL,'课堂练习：两个有序顺序表的合并',7,2,2,NULL,NULL,NULL,NULL,NULL,NULL),(108,NULL,'课堂练习：两个有序顺序表的合并',7,3,2,NULL,'88','思路正确',NULL,NULL,NULL);
-- homework: 48 rows
-- >>> end-of-statement <<<

--
-- Table `message`
--
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` int(11) NOT NULL COMMENT '接收人ID',
  `role` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接收人角色',
  `type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型：score/homework/apply/warning/attendance/course',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',
  `link` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '点击后跳转的前端路径',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读',
  `create_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '时间（yyyy-MM-dd HH:mm）',
  PRIMARY KEY (`id`),
  KEY `idx_message_receiver` (`user_id`,`role`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='站内消息';
-- message: 0 rows
-- >>> end-of-statement <<<

--
-- Table `notice`
--
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `user` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `course_id` int(11) DEFAULT NULL COMMENT '所属课程（空 = 全校通知，非空 = 课程公告）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_notice_course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='公告信息表';
INSERT INTO `notice` (`id`,`title`,`content`,`time`,`user`,`course_id`) VALUES (1,'2026 年国庆节放假安排','10 月 1 日至 7 日放假，10 月 8 日起正常上课。放假期间请注意出行安全。','2026-09-25','admin',NULL),(2,'关于开展第 8 周教学检查的通知','第 8 周将进行期中教学检查，请各位老师提前整理考勤与作业批改记录。','2026-09-30','admin',NULL),(3,'图书馆延长开放时间','自 9 月 21 日起，图书馆自习区开放至晚上 22:30。','2026-09-18','admin',NULL),(4,'2026 年秋季学期选课开始','本学期选课已开放，请同学们在规定时间内完成选课。','2026-08-25','admin',NULL),(5,'校园运动会报名通知','秋季运动会定于下月举行，有意参加的同学请到体育部报名。','2026-08-27','admin',NULL),(6,'期中考试安排已发布','各课程期中考试时间已发布在「考试安排」，请留意考场变动。','2026-10-05','admin',NULL),(7,'第 5 周课程安排','国庆后第一次课（10 月 13 日）讲二叉树的遍历，请提前预习教材第 6 章 6.1~6.3 节。','2026-09-30 18:20','路易斯',7),(8,'作业二截止时间提醒','作业二（表达式求值）已截止，还没交的同学请尽快联系我说明情况。','2026-10-05 09:00','路易斯',7),(9,'实验课改到机房','从第 6 周起，周二下午的实验课改到 7706 机房，请带好笔记本电脑和充电器。','2026-09-28 16:45','路易斯',8),(10,'实验一成绩已出','实验一已全部批改，大家可以在「作业」里查看得分和评语，有疑问课后找我。','2026-10-02 10:10','路易斯',8),(11,'习题课安排','每周四晚上 19:00 在 7701 有习题课，自愿参加。','2026-09-10 12:00','路易斯',1);
-- notice: 11 rows
-- >>> end-of-statement <<<

--
-- Table `roomplan`
--
DROP TABLE IF EXISTS `roomplan`;
CREATE TABLE `roomplan` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室编号(101-501或场馆名)',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室名称',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型:授课教室/运动场馆/固定占用',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室状态',
  `num` int(10) DEFAULT NULL COMMENT '容纳人数',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用说明',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_room_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='教室安排表';
INSERT INTO `roomplan` (`id`,`code`,`name`,`type`,`status`,`num`,`content`) VALUES (1,'7707','自习室7707','授课教室','空闲',50,'计算机教室'),(2,'7708','自习室7708','授课教室','空闲',60,'多媒体教室'),(6,'7709','器材存放教室7709','固定占用','占用',30,'器材存放'),(7,'7715','自习室7715','授课教室','空闲',13,'自习室'),(8,'7710','多媒体教室7710','授课教室','空闲',60,'多媒体教室'),(9,'7711','计算机实验室7711','授课教室','空闲',40,'计算机实验室'),(10,'7701','阶梯教室7701','授课教室','空闲',120,'阶梯教室'),(11,'7705','多媒体教室7705','授课教室','空闲',80,'多媒体教室'),(12,'7706','多媒体教室7706','授课教室','空闲',60,'多媒体教室'),(13,'TY01','篮球馆','运动场馆','空闲',80,'室内篮球馆');
-- roomplan: 10 rows
-- >>> end-of-statement <<<

--
-- Table `score`
--
DROP TABLE IF EXISTS `score`;
CREATE TABLE `score` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `attendance_score` double(10,2) DEFAULT NULL COMMENT '考勤分',
  `homework_score` double(10,2) DEFAULT NULL COMMENT '作业分',
  `ordinary_score` double(10,2) DEFAULT NULL COMMENT '平时分',
  `exam_score` double(10,2) DEFAULT NULL COMMENT '考试分',
  `score` double(10,2) DEFAULT NULL COMMENT '总成绩',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '已发布' COMMENT '草稿 / 已发布（学生只看得到已发布）',
  PRIMARY KEY (`id`) USING BTREE,
  -- 联合索引对应 ScoreMapper.selectByCourceIdAndStudentId 的精确查找；
  -- 最左前缀 course_id 同时服务「按课程过滤成绩」
  KEY `idx_score_course_student` (`course_id`,`student_id`),
  KEY `idx_score_student` (`student_id`),
  KEY `idx_score_teacher` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=98 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='成绩信息表';
INSERT INTO `score` (`id`,`student_id`,`course_id`,`teacher_id`,`attendance_score`,`homework_score`,`ordinary_score`,`exam_score`,`score`,`status`) VALUES (62,1,6,2,NULL,NULL,90.00,86.00,87.20,'已发布'),(63,2,6,2,NULL,NULL,78.00,70.00,72.40,'已发布'),(64,3,6,2,NULL,NULL,85.00,92.00,89.90,'已发布'),(65,1,14,2,NULL,NULL,92.00,88.00,89.20,'已发布'),(66,2,14,2,NULL,NULL,70.00,52.00,57.40,'已发布'),(67,4,14,2,NULL,NULL,80.00,75.00,76.50,'已发布'),(68,5,14,2,NULL,NULL,88.00,91.00,90.10,'已发布'),(69,6,14,2,NULL,NULL,75.00,66.00,68.70,'已发布'),(70,7,14,2,NULL,NULL,95.00,93.00,93.60,'已发布'),(71,8,14,2,NULL,NULL,60.00,58.00,58.60,'已发布'),(72,9,14,2,NULL,NULL,82.00,79.00,79.90,'已发布'),(73,10,14,2,NULL,NULL,77.00,81.00,79.80,'已发布'),(74,11,14,2,NULL,NULL,85.00,70.00,74.50,'已发布'),(75,1,15,4,NULL,NULL,85.00,80.00,81.50,'已发布'),(76,3,15,4,NULL,NULL,70.00,64.00,65.80,'已发布'),(77,4,15,4,NULL,NULL,66.00,72.00,70.20,'已发布'),(78,5,15,4,NULL,NULL,90.00,87.00,87.90,'已发布'),(79,6,15,4,NULL,NULL,50.00,48.00,48.60,'已发布'),(80,7,15,4,NULL,NULL,88.00,84.00,85.20,'已发布'),(81,12,15,4,NULL,NULL,79.00,83.00,81.80,'已发布'),(82,13,15,4,NULL,NULL,92.00,95.00,94.10,'已发布'),(83,11,7,2,NULL,NULL,97.00,NULL,NULL,'草稿'),(84,12,7,2,NULL,NULL,81.00,NULL,NULL,'草稿'),(85,10,7,2,NULL,NULL,84.00,NULL,NULL,'草稿'),(86,14,7,2,NULL,NULL,78.00,NULL,NULL,'草稿'),(87,2,7,2,NULL,NULL,96.00,NULL,NULL,'草稿'),(88,5,7,2,NULL,NULL,77.00,NULL,NULL,'草稿'),(89,15,7,2,NULL,NULL,91.00,NULL,NULL,'草稿'),(90,6,7,2,NULL,NULL,90.00,NULL,NULL,'草稿'),(91,3,7,2,NULL,NULL,80.00,NULL,NULL,'草稿'),(92,13,7,2,NULL,NULL,94.00,NULL,NULL,'草稿'),(93,8,7,2,NULL,NULL,87.00,NULL,NULL,'草稿'),(94,1,7,2,NULL,NULL,83.00,NULL,NULL,'草稿'),(95,4,7,2,NULL,NULL,93.00,NULL,NULL,'草稿'),(96,9,7,2,NULL,NULL,71.00,NULL,NULL,'草稿'),(97,7,7,2,NULL,NULL,74.00,NULL,NULL,'草稿');
-- score: 36 rows
-- >>> end-of-statement <<<

--
-- Table `speciality`
--
DROP TABLE IF EXISTS `speciality`;
CREATE TABLE `speciality` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业名称',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业描述',
  `college_id` int(10) DEFAULT NULL COMMENT '所属学院',
  `score` int(10) DEFAULT NULL COMMENT '学分限定',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='专业信息表';
INSERT INTO `speciality` (`id`,`name`,`content`,`college_id`,`score`) VALUES (1,'物联网工程','	\n巴拉巴拉巴拉-------',3,50),(2,'中国汉语言文学','	\n巴拉巴拉巴拉-------',4,50),(3,'材料成型及控制技术','巴拉巴拉巴拉-------',5,50),(4,'马克思主义','巴拉巴拉巴拉-------',4,50),(5,'电工电子','巴拉巴拉巴拉-------',2,50),(6,'计算机科学与技术','	\n巴拉巴拉巴拉-------',3,50);
-- speciality: 6 rows
-- >>> end-of-statement <<<

--
-- Table `student`
--
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `role` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
  `college_id` int(11) DEFAULT NULL COMMENT '学院ID',
  `speciality_id` int(11) DEFAULT NULL COMMENT '专业ID',
  `class_id` int(11) DEFAULT NULL COMMENT '班级ID',
  `score` int(11) DEFAULT '0' COMMENT '学分',
  `theme` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'system' COMMENT '主题偏好: light/dark/system',
  `locale` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'zh-CN' COMMENT '界面语言: zh-CN/en-US',
  `theme_color` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '自定义主题色: #RRGGBB，空串表示用内置默认色',
  PRIMARY KEY (`id`) USING BTREE,
  -- 用户名唯一：既是登录查询的索引，也堵住「先查后插」的账号重复竞态
  UNIQUE KEY `uk_student_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='学生信息表';
INSERT INTO `student` (`id`,`username`,`password`,`name`,`avatar`,`role`,`college_id`,`speciality_id`,`class_id`,`score`,`theme`,`locale`,`theme_color`) VALUES (1,'zhangsan','$2a$10$HWTcpOLJAiEFHAguE5nB0.1zvvYxTUVr4IX5GMZRXBwttsnv3vxwC','张三','/api/files/1782741766056-蛋白粉.png','STUDENT',3,6,5,10,'system','zh-CN',''),(2,'lisi','$2a$10$pZBRjGax7whN034u83ohX.wl1ctT3g.F8ZmBSREsNMqiuqIa18AHK','李四','/api/files/1782741760662-蛋白粉.png','STUDENT',4,4,3,2,'system','zh-CN',''),(3,'wangwu','$2a$10$qC3N4eO9Mm7ghIeDEOKgGOKRHvon7gHyYh/KBmILsbDZV6JqYLnJm','王五','/api/files/1782741753481-蛋白粉.png','STUDENT',3,1,2,6,'system','zh-CN',''),(4,'zhaoliu','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','赵六',NULL,'STUDENT',3,6,5,8,'system','zh-CN',''),(5,'qianqi','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','钱七',NULL,'STUDENT',3,6,5,8,'system','zh-CN',''),(6,'sunba','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','孙八',NULL,'STUDENT',3,6,5,4,'system','zh-CN',''),(7,'zhoujiu','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','周九',NULL,'STUDENT',3,6,5,8,'system','zh-CN',''),(8,'wushi','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','吴十',NULL,'STUDENT',3,6,5,0,'system','zh-CN',''),(9,'zhengyi','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','郑一',NULL,'STUDENT',3,6,5,4,'system','zh-CN',''),(10,'fenger','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','冯二',NULL,'STUDENT',3,6,5,4,'system','zh-CN',''),(11,'chensan','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','陈三',NULL,'STUDENT',3,6,5,4,'system','zh-CN',''),(12,'chusi','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','褚四',NULL,'STUDENT',3,1,2,4,'system','zh-CN',''),(13,'weiwu','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','卫五',NULL,'STUDENT',3,1,2,4,'system','zh-CN',''),(14,'jiangliu','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','蒋六',NULL,'STUDENT',3,1,2,0,'system','zh-CN',''),(15,'shenqi','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','沈七',NULL,'STUDENT',3,1,2,0,'system','zh-CN','');
-- student: 15 rows
-- >>> end-of-statement <<<

--
-- Table `sys_config`
--
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数键',
  `config_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数值',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='系统参数（学期名称、开学日期、教学周数）';
INSERT INTO `sys_config` (`config_key`,`config_value`,`remark`) VALUES ('semester_name','2026-2027 学年第一学期','当前学期名称'),('semester_start','2026-08-31','开学日期（第 1 教学周的周一，yyyy-MM-dd）'),('semester_weeks','18','教学周数');
-- sys_config: 3 rows
-- >>> end-of-statement <<<

--
-- Table `sys_login_log`
--
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(255) DEFAULT NULL COMMENT '登录账号',
  `ip` varchar(64) DEFAULT NULL COMMENT '登录IP',
  `status` varchar(10) DEFAULT NULL COMMENT '状态: 成功/失败',
  `msg` varchar(255) DEFAULT NULL COMMENT '说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
-- sys_login_log: 0 rows
-- >>> end-of-statement <<<

--
-- Table `sys_oper_log`
--
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(255) DEFAULT NULL COMMENT '操作人账号',
  `module` varchar(255) DEFAULT NULL COMMENT '操作模块(类#方法)',
  `type` varchar(10) DEFAULT NULL COMMENT '请求方式',
  `url` varchar(255) DEFAULT NULL COMMENT '请求地址',
  `params` varchar(600) DEFAULT NULL COMMENT '请求参数(脱敏截断)',
  `ip` varchar(64) DEFAULT NULL COMMENT '操作IP',
  `code` varchar(10) DEFAULT NULL COMMENT '响应码',
  `msg` varchar(500) DEFAULT NULL COMMENT '响应消息',
  `duration` int(11) DEFAULT NULL COMMENT '耗时(毫秒)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
-- sys_oper_log: 0 rows
-- >>> end-of-statement <<<

--
-- Table `sys_permission`
--
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限码(模块:动作)',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'menu' COMMENT '类型: menu=页面/查看 button=操作',
  `module` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属模块(权限码前缀)',
  `sort_num` int(10) NOT NULL DEFAULT '0' COMMENT '排序号(页面内同模块排序)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`),
  KEY `idx_permission_module` (`module`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='权限点表';
INSERT INTO `sys_permission` (`id`,`code`,`name`,`type`,`module`,`sort_num`) VALUES (1,'dashboard:view','数据大屏','menu','dashboard',1),(2,'college:view','学院信息-查看','menu','college',1),(3,'college:manage','学院信息-管理','button','college',2),(4,'speciality:view','专业信息-查看','menu','speciality',1),(5,'speciality:manage','专业信息-管理','button','speciality',2),(6,'classes:view','班级信息-查看','menu','classes',1),(7,'classes:manage','班级信息-管理','button','classes',2),(8,'course:view','课程信息-查看','menu','course',1),(9,'course:manage','课程信息-管理','button','course',2),(10,'choice:view','我的选课-查看','menu','choice',1),(11,'choice:manage','我的选课-操作(选课/退课/评教)','button','choice',2),(12,'score:view','成绩-查看','menu','score',1),(13,'score:manage','成绩-录入/修改/删除','button','score',2),(14,'comment:view','评教-查看','menu','comment',1),(15,'comment:manage','评教-操作(提交评教)','button','comment',2),(16,'apply:view','请假-查看','menu','apply',1),(17,'apply:manage','请假-操作(提交/撤销/审核)','button','apply',2),(18,'homework:view','作业-查看','menu','homework',1),(19,'homework:manage','作业-操作(提交/批改)','button','homework',2),(20,'attendance:view','考勤-查看','menu','attendance',1),(21,'attendance:manage','考勤-录入','button','attendance',2),(22,'notice:view','教务通知-查看','menu','notice',1),(23,'notice:manage','教务通知-管理','button','notice',2),(24,'examplan:view','考试安排-查看','menu','examplan',1),(25,'examplan:manage','考试安排-管理','button','examplan',2),(26,'roomplan:view','教室安排-查看','menu','roomplan',1),(27,'roomplan:manage','教室安排-管理','button','roomplan',2),(28,'admin:view','管理员-查看','menu','admin',1),(29,'admin:manage','管理员-管理(增删改)','button','admin',2),(30,'admin:self','管理员-修改本人资料','button','admin',3),(31,'teacher:view','教师-查看','menu','teacher',1),(32,'teacher:manage','教师-管理(增删改)','button','teacher',2),(33,'teacher:self','教师-修改本人资料','button','teacher',3),(34,'student:view','学生-查看','menu','student',1),(35,'student:manage','学生-管理(增删改)','button','student',2),(36,'student:self','学生-修改本人资料','button','student',3),(37,'student:export','学生-导出/导入','button','student',4),(38,'student:resetPwd','学生-重置密码','button','student',5),(39,'log:view','日志-查看','menu','log',1),(40,'log:manage','日志-删除','button','log',2),(41,'file:upload','文件-上传','button','file',1),(42,'file:delete','文件-删除','button','file',2),(43,'permission:manage','权限设置','menu','permission',1),(44,'config:manage','学期设置','menu','config',1),(45,'assignment:view','作业任务-查看','menu','assignment',1),(46,'assignment:manage','作业任务-布置/批改','button','assignment',2),(47,'assignment:submit','作业任务-提交作业','button','assignment',3),(48,'course:teach','课程空间-发公告/改简介','button','course',3),(49,'attendance:checkin','考勤-课堂签到','button','attendance',3),(50,'resource:view','课程资料-查看','menu','resource',1),(51,'resource:manage','课程资料-上传/删除','button','resource',2);
-- sys_permission: 51 rows
-- >>> end-of-statement <<<

--
-- Table `sys_role`
--
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色标识(与账号表 role 一致)',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `descr` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='角色表';
INSERT INTO `sys_role` (`id`,`code`,`name`,`descr`) VALUES (1,'ADMIN','管理员','系统超级管理员，拥有全部权限'),(2,'TEACHER','教师','负责授课、成绩录入、作业批改、考勤管理'),(3,'STUDENT','学生','负责选课、成绩/课表查看、请假、作业提交、评教');
-- sys_role: 3 rows
-- >>> end-of-statement <<<

--
-- Table `sys_role_permission`
--
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` int(10) NOT NULL COMMENT '角色ID',
  `permission_id` int(10) NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_rp_permission` (`permission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='角色-权限关联表';
INSERT INTO `sys_role_permission` (`id`,`role_id`,`permission_id`) VALUES (18,1,1),(12,1,2),(13,1,3),(34,1,4),(35,1,5),(10,1,6),(11,1,7),(16,1,8),(17,1,9),(8,1,10),(9,1,11),(32,1,12),(33,1,13),(14,1,14),(15,1,15),(4,1,16),(5,1,17),(23,1,18),(24,1,19),(6,1,20),(7,1,21),(27,1,22),(28,1,23),(19,1,24),(20,1,25),(30,1,26),(31,1,27),(1,1,28),(2,1,29),(3,1,30),(41,1,31),(42,1,32),(43,1,33),(36,1,34),(37,1,35),(38,1,36),(39,1,37),(40,1,38),(25,1,39),(26,1,40),(21,1,41),(22,1,42),(29,1,43),(186,1,44),(187,1,45),(190,1,46),(193,1,48),(196,1,50),(199,1,51),(169,2,8),(185,2,9),(172,2,10),(177,2,12),(178,2,13),(171,2,14),(181,2,16),(182,2,18),(175,2,19),(176,2,20),(183,2,21),(170,2,22),(180,2,24),(174,2,26),(179,2,31),(173,2,33),(184,2,41),(188,2,45),(191,2,46),(194,2,48),(197,2,50),(200,2,51),(81,3,1),(82,3,8),(83,3,10),(84,3,11),(85,3,12),(86,3,14),(87,3,15),(88,3,16),(89,3,17),(90,3,18),(91,3,19),(92,3,20),(93,3,22),(94,3,24),(95,3,26),(96,3,34),(97,3,36),(98,3,41),(189,3,45),(192,3,47),(195,3,49),(198,3,50);
-- sys_role_permission: 93 rows
-- >>> end-of-statement <<<

--
-- Table `teacher`
--
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `role` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职称',
  `theme` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'system' COMMENT '主题偏好: light/dark/system',
  `locale` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'zh-CN' COMMENT '界面语言: zh-CN/en-US',
  `theme_color` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '自定义主题色: #RRGGBB，空串表示用内置默认色',
  PRIMARY KEY (`id`) USING BTREE,
  -- 用户名唯一：既是登录查询的索引，也堵住「先查后插」的账号重复竞态
  UNIQUE KEY `uk_teacher_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='教师信息表';
INSERT INTO `teacher` (`id`,`username`,`password`,`name`,`avatar`,`role`,`phone`,`email`,`title`,`theme`,`locale`,`theme_color`) VALUES (2,'luys','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','路易斯','/api/files/1782741741320-棒球.png','TEACHER','18896188780','luys@example.edu.cn','副教授','system','zh-CN',''),(3,'chenmin','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','陈敏',NULL,'TEACHER','13800000003','chenmin@example.edu.cn','讲师','system','zh-CN',''),(4,'liuyang','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','刘洋',NULL,'TEACHER','13800000004','liuyang@example.edu.cn','教授','system','zh-CN',''),(5,'wangqiang','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','王强',NULL,'TEACHER','13800000005','wangqiang@example.edu.cn','讲师','system','zh-CN','');
-- teacher: 4 rows
-- >>> end-of-statement <<<

SET FOREIGN_KEY_CHECKS = 1;
