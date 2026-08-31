-- =============================================================
-- RBAC 权限体系：角色 / 权限点 / 角色-权限关联
-- 目标：把原先"角色是账号表上一个字符串"的硬编码权限升级为数据库可配置的权限模型。
--
-- 适用场景：
--   * 全新部署：先导入 xm_educational_manager-full.sql，再执行本脚本；
--   * 已有数据库：直接执行本脚本（幂等，可重复执行）。
--
-- 说明：
--   * 角色仅三种（与账号表 role 字段取值一致），可调整的是"角色拥有哪些权限点"；
--   * ADMIN 为超级管理员，服务端切面直接放行（本脚本仍为其写入全部权限点，便于页面展示）；
--   * 权限点采用"模块:动作"编码，如 course:manage、score:view。
-- =============================================================

DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_role`;

-- ---------- 角色 ----------
CREATE TABLE `sys_role` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色标识(与账号表 role 一致)',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `descr` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='角色表';

-- ---------- 权限点 ----------
CREATE TABLE `sys_permission` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限码(模块:动作)',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'menu' COMMENT '类型: menu=页面/查看 button=操作',
  `module` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属模块(权限码前缀)',
  `sort_num` int(10) NOT NULL DEFAULT 0 COMMENT '排序号(页面内同模块排序)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`),
  KEY `idx_permission_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='权限点表';

-- ---------- 角色-权限关联 ----------
CREATE TABLE `sys_role_permission` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` int(10) NOT NULL COMMENT '角色ID',
  `permission_id` int(10) NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_rp_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='角色-权限关联表';

-- ---------- 角色数据 ----------
INSERT INTO `sys_role` (`id`,`code`,`name`,`descr`) VALUES
(1,'ADMIN','管理员','系统超级管理员，拥有全部权限'),
(2,'TEACHER','教师','负责授课、成绩录入、作业批改、考勤管理'),
(3,'STUDENT','学生','负责选课、成绩/课表查看、请假、作业提交、评教');

-- ---------- 权限点数据 ----------
INSERT INTO `sys_permission` (`id`,`code`,`name`,`type`,`module`,`sort_num`) VALUES
(1,'dashboard:view','数据大屏','menu','dashboard',1),
(2,'college:view','学院信息-查看','menu','college',1),
(3,'college:manage','学院信息-管理','button','college',2),
(4,'speciality:view','专业信息-查看','menu','speciality',1),
(5,'speciality:manage','专业信息-管理','button','speciality',2),
(6,'classes:view','班级信息-查看','menu','classes',1),
(7,'classes:manage','班级信息-管理','button','classes',2),
(8,'course:view','课程信息-查看','menu','course',1),
(9,'course:manage','课程信息-管理','button','course',2),
(10,'choice:view','我的选课-查看','menu','choice',1),
(11,'choice:manage','我的选课-操作(选课/退课/评教)','button','choice',2),
(12,'score:view','成绩-查看','menu','score',1),
(13,'score:manage','成绩-录入/修改/删除','button','score',2),
(14,'comment:view','评教-查看','menu','comment',1),
(15,'comment:manage','评教-操作(提交评教)','button','comment',2),
(16,'apply:view','请假-查看','menu','apply',1),
(17,'apply:manage','请假-操作(提交/撤销/审核)','button','apply',2),
(18,'homework:view','作业-查看','menu','homework',1),
(19,'homework:manage','作业-操作(提交/批改)','button','homework',2),
(20,'attendance:view','考勤-查看','menu','attendance',1),
(21,'attendance:manage','考勤-录入','button','attendance',2),
(22,'notice:view','教务通知-查看','menu','notice',1),
(23,'notice:manage','教务通知-管理','button','notice',2),
(24,'examplan:view','考试安排-查看','menu','examplan',1),
(25,'examplan:manage','考试安排-管理','button','examplan',2),
(26,'roomplan:view','教室安排-查看','menu','roomplan',1),
(27,'roomplan:manage','教室安排-管理','button','roomplan',2),
(28,'admin:view','管理员-查看','menu','admin',1),
(29,'admin:manage','管理员-管理(增删改)','button','admin',2),
(30,'admin:self','管理员-修改本人资料','button','admin',3),
(31,'teacher:view','教师-查看','menu','teacher',1),
(32,'teacher:manage','教师-管理(增删改)','button','teacher',2),
(33,'teacher:self','教师-修改本人资料','button','teacher',3),
(34,'student:view','学生-查看','menu','student',1),
(35,'student:manage','学生-管理(增删改)','button','student',2),
(36,'student:self','学生-修改本人资料','button','student',3),
(37,'student:export','学生-导出/导入','button','student',4),
(38,'student:resetPwd','学生-重置密码','button','student',5),
(39,'log:view','日志-查看','menu','log',1),
(40,'log:manage','日志-删除','button','log',2),
(41,'file:upload','文件-上传','button','file',1),
(42,'file:delete','文件-删除','button','file',2),
(43,'permission:manage','权限设置','menu','permission',1);

-- ---------- 授权矩阵 ----------
-- ADMIN：全部权限点（服务端放行，此处写入便于页面展示完整勾选）
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT 1, id FROM `sys_permission`;

-- TEACHER
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
(2,1),(2,8),(2,10),(2,12),(2,13),(2,14),(2,16),(2,18),(2,19),(2,20),(2,21),(2,22),(2,24),(2,26),(2,31),(2,33),(2,41);

-- STUDENT
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
(3,1),(3,8),(3,10),(3,11),(3,12),(3,14),(3,15),(3,16),(3,17),(3,18),(3,19),(3,20),(3,22),(3,24),(3,26),(3,34),(3,36),(3,41);
