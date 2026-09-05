-- ============================================================
-- 教务管理系统 精简演示种子数据（由净化后的数据库自动导出）
-- 演示账号：admin(管理员) / luys(教师) / zhangsan、lisi、wangwu(学生)，初始密码均为 123456
-- 头像：files/ 目录仅保留 5 个在用文件，账号头像与作业附件地址为 /api/files/ 相对路径
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='管理员';
INSERT INTO `admin` (`id`,`username`,`password`,`name`,`avatar`,`role`,`phone`,`email`,`theme`,`locale`) VALUES (1,'admin','$2b$10$RL2AW18BBO.J2oje.TjxR.sABsocXXFTvC/nusjnJvyueZZxFSr5u','管理员','/api/files/7e2468d07dc47789c731faa6edbd11ea.jpg','ADMIN','12345678901','admin@xm.com','system','zh-CN');
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='请假信息表';
INSERT INTO `apply` (`id`,`student_id`,`content`,`time`,`day`,`status`,`descr`) VALUES (1,1,'我要回去过年---真的哦','2024-12-01',20,'审核通过','路上注意安全！！'),(3,1,'家中有事需要回家处理---','2024-12-05',5,'审核通过','允许申请'),(4,2,'我要回去办卡！','2024-12-07',2,'审核通过','路上要注意安全到家了给我发消息');
-- apply: 3 rows
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=92 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='考勤信息表';
INSERT INTO `attendance` (`id`,`student_id`,`teacher_id`,`course_id`,`time`,`status`) VALUES (1,1,2,6,'2024-12-10','正常'),(2,2,2,5,'2024-12-10','迟到'),(13,2,2,1,'2026-08-26','缺勤'),(21,2,2,1,'2026-08-26','缺勤'),(80,2,2,5,'2026-03-07 08:30:00','正常'),(81,2,2,5,'2026-04-03 14:00:00','正常'),(82,3,2,5,'2026-03-07 08:30:00','正常'),(83,3,2,5,'2026-04-04 14:00:00','正常'),(90,1,2,6,'2026-03-08 08:30:00','正常'),(91,1,2,6,'2026-04-02 14:00:00','正常');
-- attendance: 10 rows
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='选课信息表';
INSERT INTO `choice` (`id`,`teacher_id`,`student_id`,`course_id`) VALUES (1,2,1,6),(2,2,2,1),(4,2,2,5),(7,2,3,5);
-- choice: 4 rows
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='班级信息表';
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='学院信息表';
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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='评教信息表';
INSERT INTO `comment` (`id`,`name`,`teacher`,`student`,`content`,`time`) VALUES (7,'高等数学','路易斯','张三','讲解通俗易懂，受益匪浅。','2026-08-28 09:15:00'),(10,'高等数学','路易斯','张三','讲解通俗易懂，受益匪浅。','2026-08-28 09:15:00');
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='课程信息表';
INSERT INTO `course` (`id`,`name`,`type`,`teacher_id`,`score`,`num`,`room`,`week`,`segment`,`status`) VALUES (1,'高等数学','必修',2,5,50,'7701','星期一','第一大节（08:30 ~ 10:10）','已结课'),(5,'线性代数','必修',2,3,50,'7705','星期五','第三大节（14:00 ~ 15:40）','已结课'),(6,'中国近代史纲要','选修',2,2,50,'7706','星期五','第三大节（14:00 ~ 15:40）','已结课');
-- course: 3 rows
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='考试安排表';
INSERT INTO `examplan` (`id`,`name`,`content`,`time`) VALUES (2,'2024期末考试时间安排','今天要考数学！','2024-11-29 18:18:57'),(3,'2026年秋季期中考试安排','期中考试将于第9周举行，具体科目安排见教务通知。','2026-08-26 10:00:00'),(4,'全国计算机等级考试提醒','9月全国计算机等级考试准考证开始打印，请按时领取。','2026-08-28 09:00:00');
-- examplan: 3 rows
-- >>> end-of-statement <<<

--
-- Table `homework`
--
DROP TABLE IF EXISTS `homework`;
CREATE TABLE `homework` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程说明',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `file` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作业文件',
  `score` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打分',
  `descr` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='作业信息表';
INSERT INTO `homework` (`id`,`content`,`course_id`,`student_id`,`teacher_id`,`file`,`score`,`descr`) VALUES (1,'中国近代史纲要第二章第二节作业！',6,1,2,'/api/files/1733214242205-本科毕业设计（论文）开题报告-219981102-邓余.docx','80','还不错！'),(2,'线性代数第一章作业！',5,2,2,'/api/files/1740051505226-建议.txt',NULL,''),(12,'高等数学 第3次作业（高等数学 练习）',1,2,2,NULL,NULL,'待批改'),(42,'线性代数 第3次作业（线性代数 练习）',5,2,2,NULL,NULL,'待批改'),(43,'线性代数 第4次作业（线性代数 练习）',5,3,2,NULL,'81','已批改'),(47,'中国近代史纲要 第2次作业（中国近代史纲要 练习）',6,1,2,NULL,NULL,'待批改');
-- homework: 6 rows
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='公告信息表';
INSERT INTO `notice` (`id`,`title`,`content`,`time`,`user`) VALUES (1,'今天系统正式上线，开始内测','今天系统正式上线，开始内测','2023-09-05','admin'),(2,'所有功能都已完成，可以正常使用','所有功能都已完成，可以正常使用','2023-09-05','admin'),(3,'今天天气很不错，可以出去一起玩了','今天天气很不错，可以出去一起玩了','2023-09-05','admin'),(4,'2026年秋季学期选课开始','本学期选课系统已开放，请同学们在规定时间内完成选课。','2026-08-25','admin'),(5,'校园运动会报名通知','秋季运动会定于下月举行，有意参加的同学请到体育部报名。','2026-08-27','admin'),(6,'WebSocket 实时推送测试！','这是一条由管理员发布的通知，用于验证 App 端实时角标。','2026-09-04','admin');
-- notice: 6 rows
-- >>> end-of-statement <<<

--
-- Table `roomplan`
--
DROP TABLE IF EXISTS `roomplan`;
CREATE TABLE `roomplan` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室名称',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室状态',
  `num` int(10) DEFAULT NULL COMMENT '容纳人数',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='教室安排表';
INSERT INTO `roomplan` (`id`,`name`,`status`,`num`,`content`) VALUES (1,'自习室7707','空闲',50,'计算机教室'),(2,'自习室7708','占用',60,'多媒体教室'),(6,'器材存放教室7709','占用',30,'器材存放'),(7,'自习室7715','空闲',13,'器材存放！\n'),(8,'多媒体教室7710','空闲',60,'多媒体教室'),(9,'计算机实验室7711','占用',40,'计算机实验室');
-- roomplan: 6 rows
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
  `ordinary_score` double(10,2) DEFAULT NULL COMMENT '平时分',
  `exam_score` double(10,2) DEFAULT NULL COMMENT '考试分',
  `score` double(10,2) DEFAULT NULL COMMENT '总成绩',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='成绩信息表';
INSERT INTO `score` (`id`,`student_id`,`course_id`,`teacher_id`,`ordinary_score`,`exam_score`,`score`) VALUES (1,2,1,2,80.0,90.0,87.0),(2,1,6,2,90.0,60.0,69.0),(21,1,6,2,65.0,70.0,68.5),(22,2,6,2,55.0,60.0,58.5),(26,2,1,2,47.0,57.0,54.0),(56,2,5,2,59.0,77.0,71.6),(57,3,5,2,66.0,88.0,81.4),(61,1,6,2,55.0,71.0,66.2);
-- score: 8 rows
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='专业信息表';
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='学生信息表';
INSERT INTO `student` (`id`,`username`,`password`,`name`,`avatar`,`role`,`college_id`,`speciality_id`,`class_id`,`score`,`theme`,`locale`) VALUES (1,'zhangsan','$2a$10$HWTcpOLJAiEFHAguE5nB0.1zvvYxTUVr4IX5GMZRXBwttsnv3vxwC','张三','/api/files/1782741766056-蛋白粉.png','STUDENT',5,3,1,7,'system','zh-CN'),(2,'lisi','$2a$10$pZBRjGax7whN034u83ohX.wl1ctT3g.F8ZmBSREsNMqiuqIa18AHK','李四','/api/files/1782741760662-蛋白粉.png','STUDENT',4,4,3,5,'system','zh-CN'),(3,'wangwu','$2a$10$qC3N4eO9Mm7ghIeDEOKgGOKRHvon7gHyYh/KBmILsbDZV6JqYLnJm','王五','/api/files/1782741753481-蛋白粉.png','STUDENT',3,1,2,5,'system','zh-CN');
-- student: 3 rows
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
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
-- 日志表为运行时产物，种子只保留结构，不带数据
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
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
-- 日志表为运行时产物，种子只保留结构，不带数据
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
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='权限点表';
INSERT INTO `sys_permission` (`id`,`code`,`name`,`type`,`module`,`sort_num`) VALUES (1,'dashboard:view','数据大屏','menu','dashboard',1),(2,'college:view','学院信息-查看','menu','college',1),(3,'college:manage','学院信息-管理','button','college',2),(4,'speciality:view','专业信息-查看','menu','speciality',1),(5,'speciality:manage','专业信息-管理','button','speciality',2),(6,'classes:view','班级信息-查看','menu','classes',1),(7,'classes:manage','班级信息-管理','button','classes',2),(8,'course:view','课程信息-查看','menu','course',1),(9,'course:manage','课程信息-管理','button','course',2),(10,'choice:view','我的选课-查看','menu','choice',1),(11,'choice:manage','我的选课-操作(选课/退课/评教)','button','choice',2),(12,'score:view','成绩-查看','menu','score',1),(13,'score:manage','成绩-录入/修改/删除','button','score',2),(14,'comment:view','评教-查看','menu','comment',1),(15,'comment:manage','评教-操作(提交评教)','button','comment',2),(16,'apply:view','请假-查看','menu','apply',1),(17,'apply:manage','请假-操作(提交/撤销/审核)','button','apply',2),(18,'homework:view','作业-查看','menu','homework',1),(19,'homework:manage','作业-操作(提交/批改)','button','homework',2),(20,'attendance:view','考勤-查看','menu','attendance',1),(21,'attendance:manage','考勤-录入','button','attendance',2),(22,'notice:view','教务通知-查看','menu','notice',1),(23,'notice:manage','教务通知-管理','button','notice',2),(24,'examplan:view','考试安排-查看','menu','examplan',1),(25,'examplan:manage','考试安排-管理','button','examplan',2),(26,'roomplan:view','教室安排-查看','menu','roomplan',1),(27,'roomplan:manage','教室安排-管理','button','roomplan',2),(28,'admin:view','管理员-查看','menu','admin',1),(29,'admin:manage','管理员-管理(增删改)','button','admin',2),(30,'admin:self','管理员-修改本人资料','button','admin',3),(31,'teacher:view','教师-查看','menu','teacher',1),(32,'teacher:manage','教师-管理(增删改)','button','teacher',2),(33,'teacher:self','教师-修改本人资料','button','teacher',3),(34,'student:view','学生-查看','menu','student',1),(35,'student:manage','学生-管理(增删改)','button','student',2),(36,'student:self','学生-修改本人资料','button','student',3),(37,'student:export','学生-导出/导入','button','student',4),(38,'student:resetPwd','学生-重置密码','button','student',5),(39,'log:view','日志-查看','menu','log',1),(40,'log:manage','日志-删除','button','log',2),(41,'file:upload','文件-上传','button','file',1),(42,'file:delete','文件-删除','button','file',2),(43,'permission:manage','权限设置','menu','permission',1);
-- sys_permission: 43 rows
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='角色表';
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
) ENGINE=InnoDB AUTO_INCREMENT=185 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='角色-权限关联表';
INSERT INTO `sys_role_permission` (`id`,`role_id`,`permission_id`) VALUES (18,1,1),(12,1,2),(13,1,3),(34,1,4),(35,1,5),(10,1,6),(11,1,7),(16,1,8),(17,1,9),(8,1,10),(9,1,11),(32,1,12),(33,1,13),(14,1,14),(15,1,15),(4,1,16),(5,1,17),(23,1,18),(24,1,19),(6,1,20),(7,1,21),(27,1,22),(28,1,23),(19,1,24),(20,1,25),(30,1,26),(31,1,27),(1,1,28),(2,1,29),(3,1,30),(41,1,31),(42,1,32),(43,1,33),(36,1,34),(37,1,35),(38,1,36),(39,1,37),(40,1,38),(25,1,39),(26,1,40),(21,1,41),(22,1,42),(29,1,43),(169,2,8),(172,2,10),(177,2,12),(178,2,13),(171,2,14),(181,2,16),(182,2,18);
INSERT INTO `sys_role_permission` (`id`,`role_id`,`permission_id`) VALUES (175,2,19),(176,2,20),(183,2,21),(170,2,22),(180,2,24),(174,2,26),(179,2,31),(173,2,33),(184,2,41),(81,3,1),(82,3,8),(83,3,10),(84,3,11),(85,3,12),(86,3,14),(87,3,15),(88,3,16),(89,3,17),(90,3,18),(91,3,19),(92,3,20),(93,3,22),(94,3,24),(95,3,26),(96,3,34),(97,3,36),(98,3,41);
-- sys_role_permission: 77 rows
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='教室信息表';
INSERT INTO `teacher` (`id`,`username`,`password`,`name`,`avatar`,`role`,`phone`,`email`,`title`,`theme`,`locale`) VALUES (2,'luys','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','路易斯','/api/files/1782741741320-棒球.png','TEACHER','18896188780','2744732031@qq.com','副教授','system','zh-CN');
-- teacher: 1 rows
-- >>> end-of-statement <<<

SET FOREIGN_KEY_CHECKS = 1;
