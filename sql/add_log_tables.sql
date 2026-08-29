-- 日志表迁移脚本：操作日志 + 登录日志
-- 执行方式：mysql -uroot -p xm_educational_manager < add_log_tables.sql
-- Docker 部署会按 04-log.sql 顺序自动执行

CREATE TABLE sys_oper_log (
    id          INT AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(255)  DEFAULT NULL COMMENT '操作人账号',
    module      VARCHAR(255)  DEFAULT NULL COMMENT '操作模块(类#方法)',
    type        VARCHAR(10)   DEFAULT NULL COMMENT '请求方式',
    url         VARCHAR(255)  DEFAULT NULL COMMENT '请求地址',
    params      VARCHAR(600)  DEFAULT NULL COMMENT '请求参数(脱敏截断)',
    ip          VARCHAR(64)   DEFAULT NULL COMMENT '操作IP',
    code        VARCHAR(10)   DEFAULT NULL COMMENT '响应码',
    msg         VARCHAR(500)  DEFAULT NULL COMMENT '响应消息',
    duration    INT           DEFAULT NULL COMMENT '耗时(毫秒)',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

CREATE TABLE sys_login_log (
    id          INT AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(255) DEFAULT NULL COMMENT '登录账号',
    ip          VARCHAR(64)  DEFAULT NULL COMMENT '登录IP',
    status      VARCHAR(10)  DEFAULT NULL COMMENT '状态: 成功/失败',
    msg         VARCHAR(255) DEFAULT NULL COMMENT '说明',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志';
