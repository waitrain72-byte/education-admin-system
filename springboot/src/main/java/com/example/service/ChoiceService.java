package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.enums.SegmentEnum;
import com.example.common.enums.WeekEnum;
import com.example.entity.Account;
import com.example.entity.Choice;
import com.example.entity.Course;
import com.example.entity.Curriculum;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
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
    private CourseMapper courseMapper;

    @Override
    protected CrudMapper<Choice> getMapper() {
        return choiceMapper;
    }

    /**
     * 新增（选课）：先判断课程是否存在/是否满员，再判断与该学生已选课程是否有上课时间冲突
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Choice choice) {
        // 当前选的课：带行锁读取，锁住该课程行直到事务提交，避免并发选课同时读到「未满」而超额
        // （不存在时明确报参数错误，避免 NPE 落 500）
        Course course = courseMapper.selectByIdForUpdate(choice.getCourseId());
        if (course == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        // 1. 判断该门课是否已选满：用 >= 而不是相等判断，否则历史数据一旦超过 num 就再也拦不住
        if (course.getNum() != null && choiceMapper.countByCourseId(choice.getCourseId()) >= course.getNum()) {
            throw new CustomException(ResultCodeEnum.COURSE_NUM_ERROR);
        }
        // 2. 判断该学生所选课程与他之前选的课时间是否冲突（已结课课程不再占用时段，由 SQL 过滤）
        for (Course selected : choiceMapper.selectActiveSlotsByStudentId(choice.getStudentId())) {
            if (isSameSlot(course, selected)) {
                throw new CustomException("-1", "您之前已经选过" + selected.getName() + ", 与该门课的上课时间冲突，请重新选择");
            }
        }
        choiceMapper.insert(choice);
    }

    /** 两门课是否占用同一「星期 + 大节」时段 */
    private boolean isSameSlot(Course a, Course b) {
        return a.getWeek() != null && a.getSegment() != null
                && a.getWeek().equals(b.getWeek()) && a.getSegment().equals(b.getSegment());
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
