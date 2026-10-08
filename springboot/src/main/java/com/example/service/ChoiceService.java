package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.enums.SegmentEnum;
import com.example.common.enums.WeekEnum;
import com.example.entity.Account;
import com.example.entity.Choice;
import com.example.entity.Curriculum;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 选课信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class ChoiceService extends CrudService<Choice> {

    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private EnrollmentService enrollmentService;

    @Override
    protected CrudMapper<Choice> getMapper() {
        return choiceMapper;
    }

    /**
     * 新增（选课）：规则见 {@link EnrollmentService#enroll}。学生只能给自己选；管理员可以替学生选。
     */
    @Override
    public void add(Choice choice) {
        Account current = TokenUtils.getCurrentUser();
        Integer studentId = RoleEnum.STUDENT.name().equals(current.getRole()) ? current.getId() : choice.getStudentId();
        if (!RoleEnum.STUDENT.name().equals(current.getRole()) && !RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        enrollmentService.enroll(studentId, choice.getCourseId());
    }

    /**
     * 删除（退选）：规则见 {@link EnrollmentService#drop}。学生只能退自己的课；管理员可以替学生退。
     */
    @Override
    public void deleteById(Integer id) {
        Choice row = id == null ? null : choiceMapper.selectById(id);
        if (row == null) {
            return;
        }
        Account current = TokenUtils.getCurrentUser();
        boolean student = RoleEnum.STUDENT.name().equals(current.getRole());
        if (student && !current.getId().equals(row.getStudentId())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        if (!student && !RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        enrollmentService.drop(row.getStudentId(), row.getCourseId());
    }

    /** 批量退选：逐条走 {@link #deleteById}，每一条都做归属与规则检查 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 数据行级隔离：教师/学生只能查看自己的选课（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Choice choice) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            choice.setTeacherId(currentUser.getId());
        }
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            choice.setStudentId(currentUser.getId());
        }
    }

    /**
     * 生成对应学生的选课课表
     */
    public List<Curriculum> getCurriculum() {
        Account currentUser = TokenUtils.getCurrentUser();
        Choice choice = new Choice();
        choice.setStudentId(currentUser.getId());
        List<Choice> choiceList = choiceMapper.selectAll(choice);
        List<Curriculum> list = new ArrayList<>();
        // 第一大节（08:30 ~ 10:10）
        Curriculum first = new Curriculum();
        first.setSegment(SegmentEnum.FIRST.segment);
        processWeek(first, getWeekChoiceList(choiceList, SegmentEnum.FIRST.segment));
        list.add(first);
        // 第二大节（10:30 ~ 12:10）
        Curriculum second = new Curriculum();
        second.setSegment(SegmentEnum.SECOND.segment);
        processWeek(second, getWeekChoiceList(choiceList, SegmentEnum.SECOND.segment));
        list.add(second);
        // 第三大节（14:00 ~ 15:40）
        Curriculum third = new Curriculum();
        third.setSegment(SegmentEnum.THIRD.segment);
        processWeek(third, getWeekChoiceList(choiceList, SegmentEnum.THIRD.segment));
        list.add(third);
        // 第四大节（16:00 ~ 17:40）
        Curriculum forth = new Curriculum();
        forth.setSegment(SegmentEnum.FORTH.segment);
        processWeek(forth, getWeekChoiceList(choiceList, SegmentEnum.FORTH.segment));
        list.add(forth);
        // 第五大节（19:00 ~ 20:40）
        Curriculum fifth = new Curriculum();
        fifth.setSegment(SegmentEnum.FIFTH.segment);
        processWeek(fifth, getWeekChoiceList(choiceList, SegmentEnum.FIFTH.segment));
        list.add(fifth);
        return list;
    }

    /**
     * 筛选出当前第几大节的所有选课信息
     */
    private List<Choice> getWeekChoiceList(List<Choice> choiceList, String segment) {
        return choiceList.stream().filter(x -> x.getSegment().equals(segment)).collect(Collectors.toList());
    }

    /**
     * 处理周一到周日的数据
     */
    private void processWeek(Curriculum curriculum, List<Choice> choiceList) {
        Optional<Choice> first = choiceList.stream().filter(x -> x.getWeek().equals(WeekEnum.MONDAY.week)).findFirst();
        first.ifPresent(choice -> curriculum.setMonday(choice.getName() + " (" + choice.getTeacherName() + ")"));
        Optional<Choice> second = choiceList.stream().filter(x -> x.getWeek().equals(WeekEnum.TUESDAY.week)).findFirst();
        second.ifPresent(choice -> curriculum.setTuesday(choice.getName() + " (" + choice.getTeacherName() + ")"));
        Optional<Choice> third = choiceList.stream().filter(x -> x.getWeek().equals(WeekEnum.WEDNESDAY.week)).findFirst();
        third.ifPresent(choice -> curriculum.setWednesday(choice.getName() + " (" + choice.getTeacherName() + ")"));
        Optional<Choice> forth = choiceList.stream().filter(x -> x.getWeek().equals(WeekEnum.THURSDAY.week)).findFirst();
        forth.ifPresent(choice -> curriculum.setThursday(choice.getName() + " (" + choice.getTeacherName() + ")"));
        Optional<Choice> fifth = choiceList.stream().filter(x -> x.getWeek().equals(WeekEnum.FRIDAY.week)).findFirst();
        fifth.ifPresent(choice -> curriculum.setFriday(choice.getName() + " (" + choice.getTeacherName() + ")"));
        Optional<Choice> sixth = choiceList.stream().filter(x -> x.getWeek().equals(WeekEnum.SATURDAY.week)).findFirst();
        sixth.ifPresent(choice -> curriculum.setSaturday(choice.getName() + " (" + choice.getTeacherName() + ")"));
        Optional<Choice> seventh = choiceList.stream().filter(x -> x.getWeek().equals(WeekEnum.SUNDAY.week)).findFirst();
        seventh.ifPresent(choice -> curriculum.setSunday(choice.getName() + " (" + choice.getTeacherName() + ")"));
    }
}
