mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- MySQL dump 10.13  Distrib 5.7.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: xm_educational_manager
-- ------------------------------------------------------
-- Server version	5.7.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `xm_educational_manager`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `xm_educational_manager` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `xm_educational_manager`;

--
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
INSERT INTO `admin` VALUES (1,'admin','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','管理员','http://localhost:9091/files/1782741733439-棒球.png','ADMIN','12345678901','admin@xm.com','light','zh-CN');
/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `apply`
--

DROP TABLE IF EXISTS `apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `apply` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '请假说明',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请假时间',
  `day` int(10) DEFAULT NULL COMMENT '请假天数',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核状态',
  `descr` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='请假信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `apply`
--

LOCK TABLES `apply` WRITE;
/*!40000 ALTER TABLE `apply` DISABLE KEYS */;
INSERT INTO `apply` VALUES (1,1,'我要回去过年---真的哦','2024-12-01',20,'审核通过','路上注意安全！！'),(3,1,'家中有事需要回家处理---','2024-12-05',5,'审核通过','允许申请'),(4,2,'我要回去办卡！','2024-12-07',2,'待审核',NULL),(5,4,'身体不适需要就医','2026-08-25',1,'待审核',NULL),(6,5,'参加省级程序设计竞赛','2026-08-24',2,'审核通过','同意，注意安全'),(7,6,'家中有事需要回家处理','2026-08-22',3,'审核不通过','临近考试，不予批准'),(8,7,'回校办理学籍证明','2026-08-20',1,'审核通过','同意'),(9,4,'身体不适需要就医','2026-08-25',1,'待审核',NULL),(10,5,'参加省级程序设计竞赛','2026-08-24',2,'审核通过','同意，注意安全'),(11,6,'家中有事需要回家处理','2026-08-22',3,'审核不通过','临近考试，不予批准'),(12,7,'回校办理学籍证明','2026-08-20',1,'审核通过','同意');
/*!40000 ALTER TABLE `apply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance`
--

DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `attendance` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上课时间',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考勤状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='考勤信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance`
--

LOCK TABLES `attendance` WRITE;
/*!40000 ALTER TABLE `attendance` DISABLE KEYS */;
INSERT INTO `attendance` VALUES (1,1,2,6,'2024-12-10','正常'),(2,2,2,5,'2024-12-10','迟到'),(3,1,2,4,'2024-12-05','正常'),(4,3,2,3,'2024-12-06','早退'),(5,3,2,2,'2024-12-05','缺勤'),(6,4,3,7,'2026-08-26','正常'),(7,4,3,7,'2026-08-27','迟到'),(8,9,3,7,'2026-08-26','正常'),(9,9,3,7,'2026-08-28','早退'),(10,5,5,8,'2026-08-27','正常'),(11,10,5,8,'2026-08-27','缺勤'),(12,1,2,2,'2026-08-25','正常'),(13,2,2,1,'2026-08-26','缺勤'),(14,4,3,7,'2026-08-26','正常'),(15,4,3,7,'2026-08-27','迟到'),(16,9,3,7,'2026-08-26','正常'),(17,9,3,7,'2026-08-28','早退'),(18,5,5,8,'2026-08-27','正常'),(19,10,5,8,'2026-08-27','缺勤'),(20,1,2,2,'2026-08-25','正常'),(21,2,2,1,'2026-08-26','缺勤');
/*!40000 ALTER TABLE `attendance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `choice`
--

DROP TABLE IF EXISTS `choice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `choice` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '授课教师',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='选课信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `choice`
--

LOCK TABLES `choice` WRITE;
/*!40000 ALTER TABLE `choice` DISABLE KEYS */;
INSERT INTO `choice` VALUES (1,2,1,6),(2,2,2,1),(3,2,1,4),(4,2,2,5),(5,2,3,2),(6,2,3,3),(7,2,3,5),(8,2,1,3),(9,3,4,7),(10,3,9,7),(11,3,14,7),(12,5,5,8),(13,5,10,8),(14,5,15,8),(15,7,6,9),(16,7,11,9),(17,7,16,9),(18,2,19,1),(19,2,24,1),(20,2,29,5),(21,9,4,10),(22,5,19,8),(23,3,4,7),(24,3,9,7),(25,3,14,7),(26,5,5,8),(27,5,10,8),(28,5,15,8),(29,7,6,9),(30,7,11,9),(31,7,16,9),(32,2,19,1),(33,2,24,1),(34,2,29,5),(35,9,4,10),(36,5,19,8);
/*!40000 ALTER TABLE `choice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `classes` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级名称',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级描述',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `speciality_id` int(10) DEFAULT NULL COMMENT '专业ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='班级信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (1,'材控1班','巴拉巴拉巴拉-------',22,3),(2,'物联网1班','巴拉巴拉巴拉-------',5,1),(3,'马克思1班','巴拉巴拉巴拉-------',2,4),(4,'电子工程1班','巴拉巴拉巴拉-------',2,5),(5,'计科1班','	\n巴拉巴拉巴拉-------',15,6);
/*!40000 ALTER TABLE `classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `college`
--

DROP TABLE IF EXISTS `college`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `college` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学院名称',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学院介绍',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='学院信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `college`
--

LOCK TABLES `college` WRITE;
/*!40000 ALTER TABLE `college` DISABLE KEYS */;
INSERT INTO `college` VALUES (1,'信息工程学院','巴拉巴拉巴拉-------'),(2,'软件学院','巴拉巴拉巴拉-------'),(3,'计算机与物联网学院','巴拉巴拉巴拉-------'),(4,'马克思主义学院','巴拉巴拉巴拉-------'),(5,'材料工程学院','巴拉巴拉巴拉-------');
/*!40000 ALTER TABLE `college` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `comment` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程名称',
  `teacher` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '授课教师',
  `student` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评教学生',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '评教内容',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评教时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='评教信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment`
--

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
INSERT INTO `comment` VALUES (2,'中国近代史纲要','小邓','张三','小邓老师很好---','2024-12-02 17:01:01'),(3,'高等数学','小邓','李四','小邓老师上课很认真---','2024-12-02 17:12:53'),(4,'线性代数','小邓','王五','小邓老师上课不认真---','2024-12-02 17:15:21'),(5,'数据结构','张伟明','李明轩','张老师讲课条理清晰，例题经典！','2026-08-26 10:00:00'),(6,'数据结构','张伟明','杨欣怡','老师很负责，课后答疑很耐心。','2026-08-27 14:30:00'),(7,'高等数学','路易斯','张三','讲解通俗易懂，受益匪浅。','2026-08-28 09:15:00'),(8,'数据结构','张伟明','李明轩','张老师讲课条理清晰，例题经典！','2026-08-26 10:00:00'),(9,'数据结构','张伟明','杨欣怡','老师很负责，课后答疑很耐心。','2026-08-27 14:30:00'),(10,'高等数学','路易斯','张三','讲解通俗易懂，受益匪浅。','2026-08-28 09:15:00');
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='课程信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course`
--

LOCK TABLES `course` WRITE;
/*!40000 ALTER TABLE `course` DISABLE KEYS */;
INSERT INTO `course` VALUES (1,'高等数学','必修',2,5,50,'7701','星期一','第一大节（08:30 ~ 10:10）','已结课'),(2,'英语','必修',6,5,50,'7702','星期一','第二大节（10:30 ~ 12:10）','未开课'),(3,'大学物理','必修',16,5,50,'7703','星期二','第二大节（10:30 ~ 12:10）','未开课'),(4,'马克思主义哲学','选修',20,5,50,'7704','星期三','第三大节（14:00 ~ 15:40）','已结课'),(5,'线性代数','必修',2,3,50,'7705','星期五','第三大节（14:00 ~ 15:40）','已结课'),(6,'中国近代史纲要','选修',2,2,50,'7706','星期五','第三大节（14:00 ~ 15:40）','已结课'),(7,'数据结构','必修',3,4,50,'7707','星期二','第一大节（08:30 ~ 10:10）','已开课'),(8,'操作系统','必修',5,4,50,'7708','星期三','第一大节（08:30 ~ 10:10）','已开课'),(9,'计算机网络','选修',7,3,50,'7709','星期四','第四大节（16:00 ~ 17:40）','已开课'),(10,'软件工程','必修',9,3,50,'7710','星期五','第五大节（19:00 ~ 20:40）','未开课'),(11,'数据结构','必修',3,4,50,'7707','星期二','第一大节（08:30 ~ 10:10）','已开课'),(12,'操作系统','必修',5,4,50,'7708','星期三','第一大节（08:30 ~ 10:10）','已开课'),(13,'计算机网络','选修',7,3,50,'7709','星期四','第四大节（16:00 ~ 17:40）','已开课'),(14,'软件工程','必修',9,3,50,'7710','星期五','第五大节（19:00 ~ 20:40）','未开课');
/*!40000 ALTER TABLE `course` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `examplan`
--

DROP TABLE IF EXISTS `examplan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `examplan` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='考试安排表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `examplan`
--

LOCK TABLES `examplan` WRITE;
/*!40000 ALTER TABLE `examplan` DISABLE KEYS */;
INSERT INTO `examplan` VALUES (2,'2024期末考试时间安排','今天要考数学！','2024-11-29 18:18:57'),(3,'2026年秋季期中考试安排','期中考试将于第9周举行，具体科目安排见教务通知。','2026-08-26 10:00:00'),(4,'全国计算机等级考试提醒','9月全国计算机等级考试准考证开始打印，请按时领取。','2026-08-28 09:00:00');
/*!40000 ALTER TABLE `examplan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `homework`
--

DROP TABLE IF EXISTS `homework`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='作业信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `homework`
--

LOCK TABLES `homework` WRITE;
/*!40000 ALTER TABLE `homework` DISABLE KEYS */;
INSERT INTO `homework` VALUES (1,'中国近代史纲要第二章第二节作业！',6,1,2,'http://localhost:9090/files/1733214242205-本科毕业设计（论文）开题报告-219981102-邓余.docx','80','还不错！'),(2,'线性代数第一章作业！',5,2,2,'http://localhost:9091/files/1740051505226-建议.txt',NULL,''),(3,'英语第二章第三节',2,3,2,'http://localhost:9090/files/1733215312833-本科毕业设计（论文）开题报告-219981102-邓余.docx','90','做的不错！！'),(4,'数据结构第三章树与二叉树课后习题',7,4,3,NULL,NULL,NULL),(5,'操作系统第二章进程管理作业',8,5,5,'http://localhost:9091/files/1782741733439-棒球.png',NULL,NULL),(6,'数据结构第四章串与数组作业',7,9,3,'http://localhost:9091/files/1782741733439-棒球.png','88','完成度很高，思路清晰！'),(7,'操作系统进程调度实验报告',8,10,5,'http://localhost:9091/files/1782741733439-棒球.png','76','基本正确，注意细节'),(8,'数据结构第三章树与二叉树课后习题',7,4,3,NULL,NULL,NULL),(9,'操作系统第二章进程管理作业',8,5,5,'http://localhost:9091/files/1782741733439-棒球.png',NULL,NULL),(10,'数据结构第四章串与数组作业',7,9,3,'http://localhost:9091/files/1782741733439-棒球.png','88','完成度很高，思路清晰！'),(11,'操作系统进程调度实验报告',8,10,5,'http://localhost:9091/files/1782741733439-棒球.png','76','基本正确，注意细节');
/*!40000 ALTER TABLE `homework` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notice` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',
  `time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `user` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='公告信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
INSERT INTO `notice` VALUES (1,'今天系统正式上线，开始内测','今天系统正式上线，开始内测','2023-09-05','admin'),(2,'所有功能都已完成，可以正常使用','所有功能都已完成，可以正常使用','2023-09-05','admin'),(3,'今天天气很不错，可以出去一起玩了','今天天气很不错，可以出去一起玩了','2023-09-05','admin'),(4,'2026年秋季学期选课开始','本学期选课系统已开放，请同学们在规定时间内完成选课。','2026-08-25','admin'),(5,'校园运动会报名通知','秋季运动会定于下月举行，有意参加的同学请到体育部报名。','2026-08-27','admin');
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roomplan`
--

DROP TABLE IF EXISTS `roomplan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `roomplan` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室名称',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室状态',
  `num` int(10) DEFAULT NULL COMMENT '容纳人数',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='教室安排表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roomplan`
--

LOCK TABLES `roomplan` WRITE;
/*!40000 ALTER TABLE `roomplan` DISABLE KEYS */;
INSERT INTO `roomplan` VALUES (1,'自习室7707','空闲',50,'计算机教室'),(2,'自习室7708','占用',60,'多媒体教室'),(6,'器材存放教室7709','占用',30,'器材存放'),(7,'自习室7715','空闲',13,'器材存放！\n'),(8,'多媒体教室7710','空闲',60,'多媒体教室'),(9,'计算机实验室7711','占用',40,'计算机实验室');
/*!40000 ALTER TABLE `roomplan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `score`
--

DROP TABLE IF EXISTS `score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `score` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int(10) DEFAULT NULL COMMENT '学生ID',
  `course_id` int(10) DEFAULT NULL COMMENT '课程ID',
  `teacher_id` int(10) DEFAULT NULL COMMENT '教师ID',
  `ordinary_score` double(10,2) DEFAULT NULL COMMENT '平时分',
  `exam_score` double(10,2) DEFAULT NULL COMMENT '考试分',
  `score` double(10,2) DEFAULT NULL COMMENT '总成绩',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='成绩信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `score`
--

LOCK TABLES `score` WRITE;
/*!40000 ALTER TABLE `score` DISABLE KEYS */;
INSERT INTO `score` VALUES (1,2,1,2,80.00,90.00,87.00),(2,1,6,2,90.00,60.00,69.00),(3,1,4,2,75.00,70.00,71.50),(4,3,2,2,60.00,55.00,56.50),(5,3,3,2,80.00,75.00,76.50),(16,19,1,2,88.00,92.00,90.50),(17,24,1,2,76.00,81.00,79.50),(18,29,5,2,90.00,95.00,93.00),(19,1,4,2,70.00,75.00,73.00),(20,2,4,2,82.00,88.00,86.00),(21,1,6,2,65.00,70.00,68.50),(22,2,6,2,55.00,60.00,58.50),(23,4,7,3,90.00,94.00,92.50),(24,9,7,3,80.00,85.00,83.00),(25,5,8,5,84.00,88.00,86.50);
/*!40000 ALTER TABLE `score` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `speciality`
--

DROP TABLE IF EXISTS `speciality`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `speciality` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业名称',
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业描述',
  `college_id` int(10) DEFAULT NULL COMMENT '所属学院',
  `score` int(10) DEFAULT NULL COMMENT '学分限定',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='专业信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `speciality`
--

LOCK TABLES `speciality` WRITE;
/*!40000 ALTER TABLE `speciality` DISABLE KEYS */;
INSERT INTO `speciality` VALUES (1,'物联网工程','	\n巴拉巴拉巴拉-------',3,50),(2,'中国汉语言文学','	\n巴拉巴拉巴拉-------',4,50),(3,'材料成型及控制技术','巴拉巴拉巴拉-------',5,50),(4,'马克思主义','巴拉巴拉巴拉-------',4,50),(5,'电工电子','巴拉巴拉巴拉-------',2,50),(6,'计算机科学与技术','	\n巴拉巴拉巴拉-------',3,50);
/*!40000 ALTER TABLE `speciality` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='学生信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student`
--

LOCK TABLES `student` WRITE;
/*!40000 ALTER TABLE `student` DISABLE KEYS */;
INSERT INTO `student` VALUES (1,'zhangsan','$2a$10$5Pxu5urMtYH1DTPUrVMdHuxWlKrxkKmNQC9MU8g./5K10hMg26z7K','张三','http://localhost:9091/files/1782741766056-蛋白粉.png','STUDENT',5,3,1,7,'system','zh-CN'),(2,'lisi','123','李四','http://localhost:9091/files/1782741760662-蛋白粉.png','STUDENT',4,4,3,5,'system','zh-CN'),(3,'wangwu','123','王五','http://localhost:9091/files/1782741753481-蛋白粉.png','STUDENT',3,1,2,5,'system','zh-CN'),(4,'2024001','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','李明轩','http://localhost:9091/files/avatar-21.png','STUDENT',3,1,1,12,'system','zh-CN'),(5,'2024002','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','王雨桐','http://localhost:9091/files/avatar-22.png','STUDENT',4,2,2,15,'system','zh-CN'),(6,'2024003','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','张子豪','http://localhost:9091/files/avatar-23.png','STUDENT',4,2,3,8,'system','zh-CN'),(7,'2024004','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','刘思琪','http://localhost:9091/files/avatar-24.png','STUDENT',3,1,4,20,'system','zh-CN'),(8,'2024005','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','陈嘉伟','http://localhost:9091/files/avatar-25.png','STUDENT',3,1,5,6,'system','zh-CN'),(9,'2024006','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','杨欣怡','http://localhost:9091/files/avatar-26.png','STUDENT',3,1,1,18,'system','zh-CN'),(10,'2024007','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','赵浩然','http://localhost:9091/files/avatar-27.png','STUDENT',4,2,2,10,'system','zh-CN'),(11,'2024008','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','黄佳琪','http://localhost:9091/files/avatar-28.png','STUDENT',4,2,3,22,'system','zh-CN'),(12,'2024009','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','周子涵','http://localhost:9091/files/avatar-29.png','STUDENT',3,1,4,5,'system','zh-CN'),(13,'2024010','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','吴晓彤','http://localhost:9091/files/avatar-30.png','STUDENT',3,1,5,14,'system','zh-CN'),(14,'2024011','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','徐志远','http://localhost:9091/files/avatar-31.png','STUDENT',3,1,1,9,'system','zh-CN'),(15,'2024012','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','孙梦洁','http://localhost:9091/files/avatar-32.png','STUDENT',4,2,2,25,'system','zh-CN'),(16,'2024013','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','朱文杰','http://localhost:9091/files/avatar-33.png','STUDENT',4,2,3,11,'system','zh-CN'),(17,'2024014','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','胡雅静','http://localhost:9091/files/avatar-34.png','STUDENT',3,1,4,16,'system','zh-CN'),(18,'2024015','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','郭鹏飞','http://localhost:9091/files/avatar-35.png','STUDENT',3,1,5,7,'system','zh-CN'),(19,'2024016','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','林思彤','http://localhost:9091/files/avatar-36.png','STUDENT',3,1,1,19,'system','zh-CN'),(20,'2024017','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','何俊杰','http://localhost:9091/files/avatar-37.png','STUDENT',4,2,2,13,'system','zh-CN'),(21,'2024018','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','高心怡','http://localhost:9091/files/avatar-38.png','STUDENT',4,2,3,21,'system','zh-CN'),(22,'2024019','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','罗天佑','http://localhost:9091/files/avatar-39.png','STUDENT',3,1,4,4,'system','zh-CN'),(23,'2024020','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','郑晓岚','http://localhost:9091/files/avatar-40.png','STUDENT',3,1,5,17,'system','zh-CN'),(24,'2024021','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','梁嘉辉','http://localhost:9091/files/avatar-41.png','STUDENT',3,1,1,10,'system','zh-CN'),(25,'2024022','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','谢雨欣','http://localhost:9091/files/avatar-42.png','STUDENT',4,2,2,24,'system','zh-CN'),(26,'2024023','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','宋子墨','http://localhost:9091/files/avatar-43.png','STUDENT',4,2,3,6,'system','zh-CN'),(27,'2024024','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','唐语嫣','http://localhost:9091/files/avatar-44.png','STUDENT',3,1,4,15,'system','zh-CN'),(28,'2024025','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','韩明轩','http://localhost:9091/files/avatar-45.png','STUDENT',3,1,5,9,'system','zh-CN'),(29,'2024026','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','冯思远','http://localhost:9091/files/avatar-46.png','STUDENT',3,1,1,18,'system','zh-CN'),(30,'2024027','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','曹艺涵','http://localhost:9091/files/avatar-47.png','STUDENT',4,2,2,12,'system','zh-CN'),(31,'2024028','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','邓皓轩','http://localhost:9091/files/avatar-48.png','STUDENT',4,2,3,20,'system','zh-CN'),(32,'2024029','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','肖梦琪','http://localhost:9091/files/avatar-49.png','STUDENT',3,1,4,8,'system','zh-CN'),(33,'2024030','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','许博文','http://localhost:9091/files/avatar-50.png','STUDENT',3,1,5,26,'system','zh-CN'),(34,'2024031','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','程雨萱','http://localhost:9091/files/avatar-51.png','STUDENT',3,1,1,14,'system','zh-CN'),(35,'2024032','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','罗志强','http://localhost:9091/files/avatar-52.png','STUDENT',4,2,2,11,'system','zh-CN'),(36,'2024033','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','汪子涵','http://localhost:9091/files/avatar-53.png','STUDENT',4,2,3,19,'system','zh-CN'),(37,'2024034','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','姜俊熙','http://localhost:9091/files/avatar-54.png','STUDENT',3,1,4,7,'system','zh-CN'),(38,'2024035','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','田梦瑶','http://localhost:9091/files/avatar-55.png','STUDENT',3,1,5,23,'system','zh-CN'),(39,'2024036','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','崔浩宇','http://localhost:9091/files/avatar-56.png','STUDENT',3,1,1,16,'system','zh-CN'),(40,'2024037','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','方欣蕾','http://localhost:9091/files/avatar-57.png','STUDENT',4,2,2,5,'system','zh-CN'),(41,'2024038','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','石文轩','http://localhost:9091/files/avatar-58.png','STUDENT',4,2,3,21,'system','zh-CN'),(42,'2024039','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','廖雨婷','http://localhost:9091/files/avatar-59.png','STUDENT',3,1,4,13,'system','zh-CN'),(43,'2024040','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','姚子豪','http://localhost:9091/files/avatar-60.png','STUDENT',3,1,5,9,'system','zh-CN'),(44,'2024041','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','贺静怡','http://localhost:9091/files/avatar-61.png','STUDENT',3,1,1,22,'system','zh-CN'),(45,'2024042','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','龚浩然','http://localhost:9091/files/avatar-62.png','STUDENT',4,2,2,15,'system','zh-CN'),(46,'2024043','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','尹思颖','http://localhost:9091/files/avatar-63.png','STUDENT',4,2,3,10,'system','zh-CN'),(47,'2024044','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','钟毅轩','http://localhost:9091/files/avatar-64.png','STUDENT',3,1,4,18,'system','zh-CN'),(48,'2024045','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','谭雅文','http://localhost:9091/files/avatar-65.png','STUDENT',3,1,5,6,'system','zh-CN'),(49,'2024046','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','陆嘉豪','http://localhost:9091/files/avatar-66.png','STUDENT',3,1,1,24,'system','zh-CN'),(50,'2024047','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','汪雪莉','http://localhost:9091/files/avatar-67.png','STUDENT',4,2,2,12,'system','zh-CN'),(51,'2024048','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','蔡明杰','http://localhost:9091/files/avatar-68.png','STUDENT',4,2,3,17,'system','zh-CN'),(52,'2024049','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','汤婉婷','http://localhost:9091/files/avatar-69.png','STUDENT',3,1,4,20,'system','zh-CN'),(53,'2024050','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','樊浩淼','http://localhost:9091/files/avatar-70.png','STUDENT',3,1,5,11,'system','zh-CN');
/*!40000 ALTER TABLE `student` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_login_log`
--

DROP TABLE IF EXISTS `sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_login_log`
--

LOCK TABLES `sys_login_log` WRITE;
/*!40000 ALTER TABLE `sys_login_log` DISABLE KEYS */;
INSERT INTO `sys_login_log` VALUES (1,'admin','0:0:0:0:0:0:0:1','成功','登录成功','2026-08-29 23:09:59');
/*!40000 ALTER TABLE `sys_login_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
INSERT INTO `sys_oper_log` VALUES (1,'admin','ClassesController#updateById','PUT','/classes/update','[{\"id\":5,\"name\":\"计科1班\",\"content\":\"\\t\\n巴拉巴拉巴拉-------\",\"teacherId\":15,\"specialityId\":6,\"specialityName\":\"计算机科学与技术\"}]','0:0:0:0:0:0:0:1','200','成功',3,'2026-08-30 00:30:11'),(2,'admin','ClassesController#updateById','PUT','/classes/update','[{\"id\":4,\"name\":\"电子工程1班\",\"content\":\"巴拉巴拉巴拉-------\",\"teacherId\":2,\"specialityId\":5,\"specialityName\":\"电工电子\"}]','0:0:0:0:0:0:0:1','200','成功',4,'2026-08-30 00:30:17'),(3,'admin','ClassesController#updateById','PUT','/classes/update','[{\"id\":2,\"name\":\"物联网1班\",\"content\":\"巴拉巴拉巴拉-------\",\"teacherId\":5,\"specialityId\":1,\"teacherName\":\"路易斯\",\"specialityName\":\"物联网工程\"}]','0:0:0:0:0:0:0:1','200','成功',3,'2026-08-30 00:30:23'),(4,'admin','ClassesController#updateById','PUT','/classes/update','[{\"id\":1,\"name\":\"材控1班\",\"content\":\"巴拉巴拉巴拉-------\",\"teacherId\":22,\"specialityId\":3,\"specialityName\":\"材料成型及控制技术\"}]','0:0:0:0:0:0:0:1','200','成功',0,'2026-08-30 00:30:29'),(5,'admin','CourseController#updateById','PUT','/course/update','[{\"id\":4,\"name\":\"马克思主义哲学\",\"type\":\"选修\",\"teacherId\":20,\"score\":5,\"num\":50,\"room\":\"7704\",\"week\":\"星期三\",\"segment\":\"第三大节（14:00 ~ 15:40）\",\"status\":\"已结课\"}]','0:0:0:0:0:0:0:1','200','成功',3,'2026-08-30 00:30:41'),(6,'admin','CourseController#updateById','PUT','/course/update','[{\"id\":3,\"name\":\"大学物理\",\"type\":\"必修\",\"teacherId\":16,\"score\":5,\"num\":50,\"room\":\"7703\",\"week\":\"星期二\",\"segment\":\"第二大节（10:30 ~ 12:10）\",\"status\":\"未开课\"}]','0:0:0:0:0:0:0:1','200','成功',2,'2026-08-30 00:30:47'),(7,'admin','CourseController#updateById','PUT','/course/update','[{\"id\":2,\"name\":\"英语\",\"type\":\"必修\",\"teacherId\":6,\"score\":5,\"num\":50,\"room\":\"7702\",\"week\":\"星期一\",\"segment\":\"第二大节（10:30 ~ 12:10）\",\"status\":\"未开课\"}]','0:0:0:0:0:0:0:1','200','成功',1,'2026-08-30 00:30:52'),(8,'admin','RoomplanController#updateById','PUT','/roomplan/update','[{\"id\":7,\"name\":\"自习室7715\",\"status\":\"空闲\",\"num\":13,\"content\":\"器材存放！\\n\"}]','0:0:0:0:0:0:0:1','200','成功',3,'2026-08-30 00:41:57');
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher`
--

DROP TABLE IF EXISTS `teacher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='教室信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher`
--

LOCK TABLES `teacher` WRITE;
/*!40000 ALTER TABLE `teacher` DISABLE KEYS */;
INSERT INTO `teacher` VALUES (2,'luys','$2a$10$TUjiUaJ1IKpbDHT5qhJH0ewfoUM5tnaNHzjJCQ3ebj8OljhwTDuIy','路易斯','http://localhost:9091/files/1782741741320-棒球.png','TEACHER','18896188780','2744732031@qq.com','副教授','system','zh-CN'),(3,'t01','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','王建国','http://localhost:9091/files/avatar-1.png','TEACHER','13800001101','t01@xm.edu.cn','教授','system','zh-CN'),(4,'t02','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','李秀英','http://localhost:9091/files/avatar-2.png','TEACHER','13800001102','t02@xm.edu.cn','副教授','system','zh-CN'),(5,'t03','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','张伟明','http://localhost:9091/files/avatar-3.png','TEACHER','13800001103','t03@xm.edu.cn','讲师','system','zh-CN'),(6,'t04','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','刘桂芳','http://localhost:9091/files/avatar-4.png','TEACHER','13800001104','t04@xm.edu.cn','助教','system','zh-CN'),(7,'t05','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','陈志强','http://localhost:9091/files/avatar-5.png','TEACHER','13800001105','t05@xm.edu.cn','教授','system','zh-CN'),(8,'t06','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','杨丽娟','http://localhost:9091/files/avatar-6.png','TEACHER','13800001106','t06@xm.edu.cn','副教授','system','zh-CN'),(9,'t07','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','赵国栋','http://localhost:9091/files/avatar-7.png','TEACHER','13800001107','t07@xm.edu.cn','讲师','system','zh-CN'),(10,'t08','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','黄淑芬','http://localhost:9091/files/avatar-8.png','TEACHER','13800001108','t08@xm.edu.cn','助教','system','zh-CN'),(11,'t09','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','周文斌','http://localhost:9091/files/avatar-9.png','TEACHER','13800001109','t09@xm.edu.cn','教授','system','zh-CN'),(12,'t10','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','吴雅婷','http://localhost:9091/files/avatar-10.png','TEACHER','13800001110','t10@xm.edu.cn','副教授','system','zh-CN'),(13,'t11','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','徐海洋','http://localhost:9091/files/avatar-11.png','TEACHER','13800001111','t11@xm.edu.cn','讲师','system','zh-CN'),(14,'t12','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','孙晓梅','http://localhost:9091/files/avatar-12.png','TEACHER','13800001112','t12@xm.edu.cn','讲师','system','zh-CN'),(15,'t13','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','马俊杰','http://localhost:9091/files/avatar-13.png','TEACHER','13800001113','t13@xm.edu.cn','副教授','system','zh-CN'),(16,'t14','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','朱海燕','http://localhost:9091/files/avatar-14.png','TEACHER','13800001114','t14@xm.edu.cn','讲师','system','zh-CN'),(17,'t15','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','胡明辉','http://localhost:9091/files/avatar-15.png','TEACHER','13800001115','t15@xm.edu.cn','助教','system','zh-CN'),(18,'t16','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','郭婷婷','http://localhost:9091/files/avatar-16.png','TEACHER','13800001116','t16@xm.edu.cn','教授','system','zh-CN'),(19,'t17','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','林向东','http://localhost:9091/files/avatar-17.png','TEACHER','13800001117','t17@xm.edu.cn','副教授','system','zh-CN'),(20,'t18','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','何雪芹','http://localhost:9091/files/avatar-18.png','TEACHER','13800001118','t18@xm.edu.cn','讲师','system','zh-CN'),(21,'t19','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','高志远','http://localhost:9091/files/avatar-19.png','TEACHER','13800001119','t19@xm.edu.cn','讲师','system','zh-CN'),(22,'t20','$2a$10$BC7JerlaMHTY8WjWvvKB6usUiW/fSbLuq829Ie1i3bxGlKNL6C5im','罗美玲','http://localhost:9091/files/avatar-20.png','TEACHER','13800001120','t20@xm.edu.cn','助教','system','zh-CN');
/*!40000 ALTER TABLE `teacher` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-29 18:51:16
