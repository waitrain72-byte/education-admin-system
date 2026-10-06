package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Notice;
import com.example.mapper.CourseMapper;
import com.example.mapper.NoticeMapper;
import com.example.mapper.SearchMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局搜索（顶栏 Ctrl K 命令面板的数据源）。
 *
 * <p>范围按角色裁剪：教师只搜得到自己开的课；学生能搜到全部课程（相当于在课程广场里找课）；
 * 学生与教师名单只对管理员开放。页面跳转类结果（如「修改密码」）由前端本地匹配，不经过这里。</p>
 */
@Service
public class SearchService {

    /** 关键字长度上限：命令面板是找东西用的，过长的输入直接视为无结果 */
    static final int MAX_KEYWORD_LENGTH = 30;
    private static final int COURSE_LIMIT = 6;
    private static final int NOTICE_LIMIT = 5;

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private SearchMapper searchMapper;

    public Map<String, Object> search(String keyword) {
        String kw = normalize(keyword);
        Map<String, Object> result = new LinkedHashMap<>();
        if (kw == null) {
            result.put("courses", Collections.emptyList());
            result.put("notices", Collections.emptyList());
            result.put("students", Collections.emptyList());
            result.put("teachers", Collections.emptyList());
            return result;
        }
        Account current = TokenUtils.getCurrentUser();
        String role = current.getRole();

        Course courseProbe = new Course();
        courseProbe.setName(kw);
        if (RoleEnum.TEACHER.name().equals(role)) {
            courseProbe.setTeacherId(current.getId());
        }
        PageHelper.startPage(1, COURSE_LIMIT, false);
        result.put("courses", courseMapper.selectAll(courseProbe));

        Notice noticeProbe = new Notice();
        noticeProbe.setTitle(kw);
        PageHelper.startPage(1, NOTICE_LIMIT, false);
        result.put("notices", noticeMapper.selectAll(noticeProbe));

        boolean isAdmin = RoleEnum.ADMIN.name().equals(role);
        result.put("students", isAdmin ? searchMapper.searchStudents(kw) : Collections.emptyList());
        result.put("teachers", isAdmin ? searchMapper.searchTeachers(kw) : Collections.emptyList());
        return result;
    }

    /** 去掉首尾空白；空串或超长返回 null（不查库） */
    static String normalize(String keyword) {
        String kw = StrUtil.trim(keyword);
        if (StrUtil.isEmpty(kw) || kw.length() > MAX_KEYWORD_LENGTH) {
            return null;
        }
        return kw;
    }
}
