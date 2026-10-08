package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.CourseResource;
import com.example.exception.CustomException;
import com.example.mapper.CourseResourceMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 课程资料：任课教师（或管理员）上传课件、讲义等，选课学生下载。
 *
 * <p>文件本身走通用上传接口（/files/upload），这里只登记名称、地址和大小。删除资料只删登记，不删文件：
 * 上传接口按内容去重，同一个文件可能同时被作业附件等其他地方引用。</p>
 */
@Service
public class CourseResourceService {

    static final int MAX_NAME = 200;

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private CourseSpaceService courseSpaceService;
    @Resource
    private CourseResourceMapper courseResourceMapper;
    @Resource
    private UploadedFiles uploadedFiles;

    public List<CourseResource> list(Integer courseId) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireMember(course);
        return courseResourceMapper.selectByCourse(courseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseResource add(Integer courseId, CourseResource form) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        String name = form == null ? null : StrUtil.trimToNull(form.getName());
        String file = form == null ? null : StrUtil.trimToNull(form.getFile());
        if (name == null || file == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        uploadedFiles.require(file);
        Account current = TokenUtils.getCurrentUser();
        CourseResource resource = new CourseResource();
        resource.setCourseId(courseId);
        resource.setName(StrUtil.maxLength(name, MAX_NAME));
        resource.setFile(file);
        resource.setSize(form.getSize() == null || form.getSize() < 0 ? null : form.getSize());
        resource.setUploaderId(current.getId());
        resource.setUploaderRole(current.getRole());
        resource.setCreateTime(LocalDateTime.now(AppTime.clock()).format(MINUTES));
        courseResourceMapper.insert(resource);
        resource.setUploaderName(current.getName());
        return resource;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer courseId, Integer resourceId) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        CourseResource resource = resourceId == null ? null : courseResourceMapper.selectById(resourceId);
        if (resource == null || !courseId.equals(resource.getCourseId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        courseResourceMapper.deleteById(resourceId);
    }
}
