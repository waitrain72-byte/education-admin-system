package com.example.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.mapper.AdminMapper;
import com.example.mapper.StudentMapper;
import com.example.mapper.TeacherMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class FileService {

    private static final String FILE_URL_PREFIX = "/files/";
    private static final String FILE_PATH = System.getProperty("user.dir") + "/files/";

    @Resource
    private AdminMapper adminMapper;
    @Resource
    private TeacherMapper teacherMapper;
    @Resource
    private StudentMapper studentMapper;

    public void deleteAvatarIfUnused(String oldAvatar, String newAvatar) {
        if (StrUtil.isBlank(oldAvatar) || oldAvatar.equals(newAvatar)) {
            return;
        }
        if (isAvatarInUse(oldAvatar)) {
            return;
        }
        deleteStoredFile(oldAvatar);
    }

    private boolean isAvatarInUse(String avatar) {
        return adminMapper.countByAvatar(avatar) > 0
                || teacherMapper.countByAvatar(avatar) > 0
                || studentMapper.countByAvatar(avatar) > 0;
    }

    private void deleteStoredFile(String fileUrl) {
        String fileName = extractFileName(fileUrl);
        if (StrUtil.isBlank(fileName)) {
            return;
        }

        File baseDir = new File(FILE_PATH);
        File file = new File(baseDir, fileName);
        try {
            String basePath = baseDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (filePath.startsWith(basePath + File.separator)) {
                FileUtil.del(file);
            }
        } catch (Exception ignored) {
        }
    }

    private String extractFileName(String fileUrl) {
        try {
            String path = URI.create(fileUrl).getPath();
            int index = path.lastIndexOf(FILE_URL_PREFIX);
            if (index < 0) {
                return null;
            }
            return URLDecoder.decode(path.substring(index + FILE_URL_PREFIX.length()), StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return null;
        }
    }
}
