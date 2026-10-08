package com.example.common.enums;
        /*
        枚举
        */
public enum ResultCodeEnum {
    SUCCESS("200", "成功"),
    PARAM_ERROR("400", "参数异常"),
    TOKEN_INVALID_ERROR("401", "无效的token"),
    TOKEN_CHECK_ERROR("401", "token验证失败，请重新登录"),
    PARAM_LOST_ERROR("4001", "参数缺失"),
    SYSTEM_ERROR("500", "系统异常"),
    USER_EXIST_ERROR("5001", "用户名已存在"),
    USER_NOT_LOGIN("5002", "用户未登录"),
    USER_ACCOUNT_ERROR("5003", "账号或密码错误"),
    USER_NOT_EXIST_ERROR("5004", "用户不存在"),
    PARAM_PASSWORD_ERROR("5005", "原密码输入错误"),
    COURSE_NUM_ERROR("5006","该门课选课人数已满，请选择其他课程"),
    SCORE_ALREADY_ERROR("5007","您已经录入了该学生该门课的成绩"),
    COMMENT_ALREADY_ERROR("5008","您已经对该门课的老师评教过了，请勿重复评教"),
    ATTENDANCE_ALREADY_ERROR("5009","该学生当天的考勤已经录入，请勿重复录入"),
    ROOM_OCCUPIED_ERROR("5010","该教室在此时间段已被其他课程占用"),
    ROOM_CODE_EXIST_ERROR("5011","教室编号已存在"),
    ROLE_REQUIRED_ERROR("5012","该账号对应多个身份，请选择登录身份"),
    GRADE_WEIGHT_ERROR("5013","四项权重需为 0~100 的整数，且合计正好 100"),
    SCORE_RANGE_ERROR("5014","成绩需在 0~100 之间"),
    SIGN_NOT_OPEN_ERROR("5015","这门课当前没有进行中的签到"),
    SIGN_CODE_ERROR("5016","签到码不正确"),
    SIGN_LOCKED_ERROR("5017","签到码错误次数过多，请找老师补签"),
    ASSIGNMENT_CLOSED_ERROR("5018","已过截止时间，不能再提交"),
    ASSIGNMENT_GRADED_ERROR("5019","作业已批改，不能再修改提交"),
    EVAL_NOT_OPEN_ERROR("5020","课程结课后才能评价"),
    EVAL_ALREADY_ERROR("5021","你已经评价过这门课了"),
    HOMEWORK_SCORE_ERROR("5022","分数需在 0 到这份作业的满分之间"),
    CAPTCHA_ERROR("402", "验证码错误"),
    PERMISSION_DENIED_ERROR("403", "无权限执行该操作"),
    ;

    public String code;
    public String msg;

    ResultCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
