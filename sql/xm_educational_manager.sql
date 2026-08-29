/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50728
 Source Host           : localhost:3306
 Source Schema         : xm_educational_manager

 Target Server Type    : MySQL
 Target Server Version : 50728
 File Encoding         : 65001

 Date: 06/07/2026 14:11:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色标识',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', '$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im', '管理员', 'http://localhost:9091/files/1782741733439-棒球.png', 'ADMIN', '12345678901', 'admin@xm.com');

-- ----------------------------
-- Table structure for apply
-- ----------------------------
DROP TABLE IF EXISTS `apply`;
CREATE TABLE `apply`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '请假说明',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请假时间',
  `day` int(10) DEFAULT NULL COMMENT '请假天数',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核状态',
  `descr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '请假信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of apply
-- ----------------------------
INSERT INTO `apply` VALUES (1, 1, '我要回去过年---真的哦', '2024-12-01', 20, '审核通过', '路上注意安全！！');
INSERT INTO `apply` VALUES (3, 1, '家中有事需要回家处理---', '2024-12-05', 5, '审核通过', '允许申请');
INSERT INTO `apply` VALUES (4, 2, '我要回去办卡！', '2024-12-07', 2, '待审核', NULL);

-- ----------------------------
-- Table structure for attendance
-- ----------------------------
DROP TABLE IF EXISTS `attendance`;
CREATE TABLE `attendance`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上课时间',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考勤状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考勤信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendance
-- ----------------------------
INSERT INTO `attendance` VALUES (1, 1, 2, 2, '2024-12-10', '正常');
INSERT INTO `attendance` VALUES (2, 2, 2, 2, '2024-12-10', '迟到');
INSERT INTO `attendance` VALUES (3, 1, 1, 2, '2024-12-05', '正常');
INSERT INTO `attendance` VALUES (4, 3, 1, 2, '2024-12-06', '早退');
INSERT INTO `attendance` VALUES (5, 3, 1, 2, '2024-12-05', '缺勤');

-- ----------------------------
-- Table structure for choice
-- ----------------------------
DROP TABLE IF EXISTS `choice`;
CREATE TABLE `choice`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '授课教师',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '选课信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of choice
-- ----------------------------
INSERT INTO `choice` VALUES (1, 2, 1, 6);
INSERT INTO `choice` VALUES (2, 2, 2, 1);
INSERT INTO `choice` VALUES (3, 2, 1, 4);
INSERT INTO `choice` VALUES (4, 2, 2, 5);
INSERT INTO `choice` VALUES (5, 2, 3, 2);
INSERT INTO `choice` VALUES (6, 2, 3, 3);
INSERT INTO `choice` VALUES (7, 2, 3, 5);
INSERT INTO `choice` VALUES (8, 2, 1, 3);

-- ----------------------------
-- Table structure for classes
-- ----------------------------
DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级名称',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级描述',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `speciality_id` int(10) DEFAULT NULL COMMENT '专业ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '班级信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of classes
-- ----------------------------
INSERT INTO `classes` VALUES (1, '材控1班', '巴拉巴拉巴拉-------', 1, 3);
INSERT INTO `classes` VALUES (2, '物联网1班', '巴拉巴拉巴拉-------', 2, 1);
INSERT INTO `classes` VALUES (3, '马克思1班', '巴拉巴拉巴拉-------', 2, 4);
INSERT INTO `classes` VALUES (4, '电子工程1班', '巴拉巴拉巴拉-------', 1, 5);
INSERT INTO `classes` VALUES (5, '计科1班', '	\n巴拉巴拉巴拉-------', 1, 6);

-- ----------------------------
-- Table structure for college
-- ----------------------------
DROP TABLE IF EXISTS `college`;
CREATE TABLE `college`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学院名称',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学院介绍',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学院信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of college
-- ----------------------------
INSERT INTO `college` VALUES (1, '信息工程学院', '巴拉巴拉巴拉-------');
INSERT INTO `college` VALUES (2, '软件学院', '巴拉巴拉巴拉-------');
INSERT INTO `college` VALUES (3, '计算机与物联网学院', '巴拉巴拉巴拉-------');
INSERT INTO `college` VALUES (4, '马克思主义学院', '巴拉巴拉巴拉-------');
INSERT INTO `college` VALUES (5, '材料工程学院', '巴拉巴拉巴拉-------');

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程名称',
  `teacher` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '授课教师',
  `student` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评教学生',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '评教内容',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评教时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评教信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comment
-- ----------------------------
INSERT INTO `comment` VALUES (2, '中国近代史纲要', '小邓', '张三', '小邓老师很好---', '2024-12-02 17:01:01');
INSERT INTO `comment` VALUES (3, '高等数学', '小邓', '李四', '小邓老师上课很认真---', '2024-12-02 17:12:53');
INSERT INTO `comment` VALUES (4, '线性代数', '小邓', '王五', '小邓老师上课不认真---', '2024-12-02 17:15:21');

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程名称',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程类型',
  `teacher_id` int(10) DEFAULT NULL COMMENT '授课教师',
  `score` int(10) DEFAULT NULL COMMENT '课程学分',
  `num` int(10) DEFAULT NULL COMMENT '上课人数',
  `room` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上课教室',
  `week` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '周几',
  `segment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第几大节',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上课状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course
-- ----------------------------
INSERT INTO `course` VALUES (1, '高等数学', '必修', 2, 5, 50, '7701', '星期一', '第一大节（08:30 ~ 10:10）', '已结课');
INSERT INTO `course` VALUES (2, '英语', '必修', 2, 5, 50, '7702', '星期一', '第二大节（10:30 ~ 12:10）', '未开课');
INSERT INTO `course` VALUES (3, '大学物理', '必修', 2, 5, 50, '7703', '星期二', '第二大节（10:30 ~ 12:10）', '未开课');
INSERT INTO `course` VALUES (4, '马克思主义哲学', '选修', 2, 5, 50, '7704', '星期三', '第三大节（14:00 ~ 15:40）', '已结课');
INSERT INTO `course` VALUES (5, '线性代数', '必修', 2, 3, 50, '7705', '星期五', '第三大节（14:00 ~ 15:40）', '已结课');
INSERT INTO `course` VALUES (6, '中国近代史纲要', '选修', 2, 2, 50, '7706', '星期五', '第三大节（14:00 ~ 15:40）', '已结课');

-- ----------------------------
-- Table structure for examplan
-- ----------------------------
DROP TABLE IF EXISTS `examplan`;
CREATE TABLE `examplan`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考试安排表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of examplan
-- ----------------------------
INSERT INTO `examplan` VALUES (2, '2024期末考试时间安排', '今天要考数学！', '2024-11-29 18:18:57');

-- ----------------------------
-- Table structure for homework
-- ----------------------------
DROP TABLE IF EXISTS `homework`;
CREATE TABLE `homework`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程说明',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `file` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作业文件',
  `score` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打分',
  `descr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作业信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of homework
-- ----------------------------
INSERT INTO `homework` VALUES (1, '中国近代史纲要第二章第二节作业！', 6, 1, 2, 'http://localhost:9090/files/1733214242205-本科毕业设计（论文）开题报告-219981102-邓余.docx', '80', '还不错！');
INSERT INTO `homework` VALUES (2, '线性代数第一章作业！', 5, 2, 2, 'http://localhost:9091/files/1740051505226-建议.txt', NULL, '');
INSERT INTO `homework` VALUES (3, '英语第二章第三节', 2, 3, 2, 'http://localhost:9090/files/1733215312833-本科毕业设计（论文）开题报告-219981102-邓余.docx', '90', '做的不错！！');

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `user` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '公告信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (1, '今天系统正式上线，开始内测', '今天系统正式上线，开始内测', '2023-09-05', 'admin');
INSERT INTO `notice` VALUES (2, '所有功能都已完成，可以正常使用', '所有功能都已完成，可以正常使用', '2023-09-05', 'admin');
INSERT INTO `notice` VALUES (3, '今天天气很不错，可以出去一起玩了', '今天天气很不错，可以出去一起玩了', '2023-09-05', 'admin');

-- ----------------------------
-- Table structure for roomplan
-- ----------------------------
DROP TABLE IF EXISTS `roomplan`;
CREATE TABLE `roomplan`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室名称',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室状态',
  `num` int(10) DEFAULT NULL COMMENT '容纳人数',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教室安排表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of roomplan
-- ----------------------------
INSERT INTO `roomplan` VALUES (1, '自习室7707', '空闲', 50, '计算机教室');
INSERT INTO `roomplan` VALUES (2, '自习室7708', '占用', 60, '多媒体教室');
INSERT INTO `roomplan` VALUES (6, '器材存放教室7709', '占用', 30, '器材存放');
INSERT INTO `roomplan` VALUES (7, '12', '空闲', 13, '器材存放！\n');

-- ----------------------------
-- Table structure for score
-- ----------------------------
DROP TABLE IF EXISTS `score`;
CREATE TABLE `score`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `ordinary_score` double(10, 2) DEFAULT NULL COMMENT '平时分',
  `exam_score` double(10, 2) DEFAULT NULL COMMENT '考试分',
  `score` double(10, 2) DEFAULT NULL COMMENT '总成绩',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成绩信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of score
-- ----------------------------
INSERT INTO `score` VALUES (1, 2, 1, 2, 80.00, 90.00, 87.00);
INSERT INTO `score` VALUES (2, 1, 6, 2, 90.00, 60.00, 69.00);
INSERT INTO `score` VALUES (3, 1, 4, 2, 75.00, 70.00, 71.50);
INSERT INTO `score` VALUES (4, 3, 2, 2, 60.00, 55.00, 56.50);
INSERT INTO `score` VALUES (5, 3, 3, 2, 80.00, 75.00, 76.50);

-- ----------------------------
-- Table structure for speciality
-- ----------------------------
DROP TABLE IF EXISTS `speciality`;
CREATE TABLE `speciality`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业名称',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业描述',
  `college_id` int(10) DEFAULT NULL COMMENT '所属学院',
  `score` int(10) DEFAULT NULL COMMENT '学分限定',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专业信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of speciality
-- ----------------------------
INSERT INTO `speciality` VALUES (1, '物联网工程', '	\n巴拉巴拉巴拉-------', 3, 50);
INSERT INTO `speciality` VALUES (2, '中国汉语言文学', '	\n巴拉巴拉巴拉-------', 4, 50);
INSERT INTO `speciality` VALUES (3, '材料成型及控制技术', '巴拉巴拉巴拉-------', 5, 50);
INSERT INTO `speciality` VALUES (4, '马克思主义', '巴拉巴拉巴拉-------', 4, 50);
INSERT INTO `speciality` VALUES (5, '电工电子', '巴拉巴拉巴拉-------', 2, 50);
INSERT INTO `speciality` VALUES (6, '计算机科学与技术', '	\n巴拉巴拉巴拉-------', 3, 50);

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
  `college_id` int(11) DEFAULT NULL COMMENT '学院ID',
  `speciality_id` int(11) DEFAULT NULL COMMENT '专业ID',
  `class_id` int(11) DEFAULT NULL COMMENT '班级ID',
  `score` int(11) DEFAULT 0 COMMENT '学分',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (1, 'zhangsan', '123', '张三', 'http://localhost:9091/files/1782741766056-蛋白粉.png', 'STUDENT', 5, 3, 1, 7);
INSERT INTO `student` VALUES (2, 'lisi', '123', '李四', 'http://localhost:9091/files/1782741760662-蛋白粉.png', 'STUDENT', 4, 4, 3, 5);
INSERT INTO `student` VALUES (3, 'wangwu', '123', '王五', 'http://localhost:9091/files/1782741753481-蛋白粉.png', 'STUDENT', 3, 1, 2, 5);

-- ----------------------------
-- Table structure for teacher
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教室信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teacher
-- ----------------------------
INSERT INTO `teacher` VALUES (2, 'luys', '$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy', '路易斯', 'http://localhost:9091/files/1782741741320-棒球.png', 'TEACHER', '18896188780', '2744732031@qq.com', '副教授');

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 外键列索引：加速按学院/专业/班级/教师/学生/课程维度的关联查询与筛选
-- ----------------------------
CREATE INDEX idx_apply_student_id ON `apply` (`student_id`);

CREATE INDEX idx_attendance_student_id ON `attendance` (`student_id`);
CREATE INDEX idx_attendance_teacher_id ON `attendance` (`teacher_id`);
CREATE INDEX idx_attendance_course_id ON `attendance` (`course_id`);

CREATE INDEX idx_choice_teacher_id ON `choice` (`teacher_id`);
CREATE INDEX idx_choice_student_id ON `choice` (`student_id`);
CREATE INDEX idx_choice_course_id ON `choice` (`course_id`);

CREATE INDEX idx_classes_teacher_id ON `classes` (`teacher_id`);
CREATE INDEX idx_classes_speciality_id ON `classes` (`speciality_id`);

CREATE INDEX idx_course_teacher_id ON `course` (`teacher_id`);

CREATE INDEX idx_homework_course_id ON `homework` (`course_id`);
CREATE INDEX idx_homework_student_id ON `homework` (`student_id`);
CREATE INDEX idx_homework_teacher_id ON `homework` (`teacher_id`);

CREATE INDEX idx_score_student_id ON `score` (`student_id`);
CREATE INDEX idx_score_course_id ON `score` (`course_id`);
CREATE INDEX idx_score_teacher_id ON `score` (`teacher_id`);

CREATE INDEX idx_speciality_college_id ON `speciality` (`college_id`);

CREATE INDEX idx_student_college_id ON `student` (`college_id`);
CREATE INDEX idx_student_speciality_id ON `student` (`speciality_id`);
CREATE INDEX idx_student_class_id ON `student` (`class_id`);
