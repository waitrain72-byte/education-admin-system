package com.example.task;

import com.example.mapper.LoginLogMapper;
import com.example.mapper.OperLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时任务：日志过期清理。
 * 每天凌晨 2 点清理 90 天前的操作日志与登录日志，防止日志表无限增长。
 */
@Component
public class LogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(LogCleanupTask.class);

    /** 日志保留天数 */
    private static final int RETENTION_DAYS = 90;

    @Resource
    private OperLogMapper operLogMapper;
    @Resource
    private LoginLogMapper loginLogMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredLogs() {
        int oper = operLogMapper.deleteOlderThanDays(RETENTION_DAYS);
        int login = loginLogMapper.deleteOlderThanDays(RETENTION_DAYS);
        log.info("日志定时清理完成：操作日志删除 {} 条，登录日志删除 {} 条", oper, login);
    }
}
