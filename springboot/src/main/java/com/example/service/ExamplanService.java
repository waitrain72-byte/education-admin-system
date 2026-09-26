package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Examplan;
import com.example.exception.CustomException;
import com.example.mapper.CrudMapper;
import com.example.mapper.ExamplanMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * 考试安排表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class ExamplanService extends CrudService<Examplan> {

    /** 考试时间格式（与 Web / 小程序的日期时间选择器一致）；STRICT 拒绝 2 月 30 日、24:00 这类不存在的时间 */
    private static final DateTimeFormatter EXAM_TIME_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    @Resource
    private ExamplanMapper examplanMapper;

    @Override
    protected CrudMapper<Examplan> getMapper() {
        return examplanMapper;
    }

    /**
     * 新增：校验考试时间，生成发布时间
     */
    @Override
    public void add(Examplan examplan) {
        checkExamTime(examplan.getExamTime());
        examplan.setTime(DateUtil.now());
        examplanMapper.insert(examplan);
    }

    /**
     * 修改：校验考试时间；发布时间只在新增时生成，修改时不允许改写
     */
    @Override
    public void updateById(Examplan examplan) {
        checkExamTime(examplan.getExamTime());
        examplan.setTime(null);
        super.updateById(examplan);
    }

    /**
     * 考试时间是否合法：为空时放行（历史数据没有考试时间，前端不显示倒计时）；
     * 填写了就必须是真实存在的 yyyy-MM-dd HH:mm。
     */
    static boolean isValidExamTime(String examTime) {
        if (StrUtil.isBlank(examTime)) {
            return true;
        }
        try {
            LocalDateTime.parse(examTime, EXAM_TIME_FORMAT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void checkExamTime(String examTime) {
        if (!isValidExamTime(examTime)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
    }
}
