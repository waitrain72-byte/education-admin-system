package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 附件地址校验：作业附件、作业提交、课程资料里登记的文件地址，只接受本系统上传接口（/files/upload）返回的地址。
 *
 * <p>这些地址会渲染成页面上的下载链接，若放任前端随便传，学生交一个 javascript: 开头的「附件」，
 * 老师一点就会在老师的登录态下执行脚本。</p>
 */
@Component
public class UploadedFiles {

    private static final int MAX_LENGTH = 255;

    @Value("${files.url-prefix:/api/files/}")
    private String prefix = "/api/files/";

    /** 「前缀 + 文件名」，文件名里不能再带路径 */
    public boolean isUploaded(String url) {
        if (url == null || url.length() > MAX_LENGTH || !url.startsWith(prefix)) {
            return false;
        }
        String name = url.substring(prefix.length());
        return !name.isEmpty() && !name.contains("/") && !name.contains("\\") && !name.contains("..");
    }

    /** 为空原样返回；不是本系统上传的地址报参数错误 */
    public String require(String url) {
        if (url == null) {
            return null;
        }
        if (!isUploaded(url)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return url;
    }
}
