package com.example.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/files")
public class FileController {

    /**
     * 上传的文件按「内容 MD5 + 扩展名」命名，同一个名字的内容永远不变，浏览器可以放心长期缓存：
     * 头像、作业附件换页时不用重复下载。private：只让用户自己的浏览器缓存，不进共享代理。
     */
    static final String CACHE_CONTROL = "private, max-age=31536000, immutable";

    // 文件上传存储路径（非 final 仅为单元测试能换成临时目录）
    private String filePath = System.getProperty("user.dir") + "/files/";

    /**
     * 上传扩展名白名单：业务只需要 图片（头像）+ 常见文档/压缩包（作业附件）。
     * 无扩展名或不在名单内的一律拒绝，防止上传可执行脚本等危险文件。
     */
    private static final Set<String> ALLOWED_SUFFIXES = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md",
            "zip", "rar", "7z"));

    /**
     * 文件访问 URL 前缀：默认 /api/files/，本地开发（Vite 代理）与容器部署（nginx 反代）均可直接访问；
     * 也可通过配置文件 files.url-prefix 覆盖。
     */
    @Value("${files.url-prefix:/api/files/}")
    private String fileUrlPrefix;

    /**
     * 文件上传
     */
    @RequirePermission("file:upload")
    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        if (file == null || StrUtil.isBlank(file.getOriginalFilename())) {
            return Result.error("400", "请选择文件");
        }
        String fileName = file.getOriginalFilename();
        String suffix = FileUtil.extName(fileName);
        // 扩展名白名单校验：无扩展名或不在名单内直接拒绝
        if (StrUtil.isBlank(suffix) || !ALLOWED_SUFFIXES.contains(suffix.toLowerCase())) {
            return Result.error("400", "不支持的文件类型：仅允许图片、常见文档与压缩包");
        }
        String storedName;
        try {
            byte[] bytes = file.getBytes();
            String md5 = SecureUtil.md5().digestHex(bytes);
            storedName = md5 + "." + suffix.toLowerCase();
            if (!FileUtil.isDirectory(filePath)) {
                FileUtil.mkdir(filePath);
            }
            if (!FileUtil.exist(filePath + storedName)) {
                FileUtil.writeBytes(bytes, filePath + storedName);
            }
            System.out.println(fileName + "--上传成功");
        } catch (Exception e) {
            System.err.println(fileName + "--文件上传失败");
            return Result.error("500", "文件上传失败");
        }
        return Result.success(fileUrlPrefix + storedName);
    }

    /**
     * 获取文件：边读边写（20MB 的附件不整份读进内存），带长期缓存头；文件不存在返回 404。
     */
    @GetMapping("/{flag}")
    public void avatarPath(@PathVariable String flag, HttpServletResponse response) {
        File targetFile = getSafeFile(flag);
        if (targetFile == null || !targetFile.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            // 必须按真实扩展名返回图片 MIME（image/png 等）：
            // 微信真机的原生图片组件只渲染 image/*，收到 application/octet-stream 会拒绝显示（空白）；
            // 开发者工具模拟器是 Chromium 内核会内容嗅探，所以模拟器正常、真机空白——不能以模拟器表现为准。
            // 同时不能带 Content-Disposition: attachment（附件语义，真机同样不渲染）。
            MediaType mediaType = MediaTypeFactory.getMediaType(flag).orElse(MediaType.APPLICATION_OCTET_STREAM);
            response.setContentType(mediaType.toString());
            response.setContentLengthLong(targetFile.length());
            response.setHeader("Cache-Control", CACHE_CONTROL);
            // 按声明的类型处理，不让浏览器把上传的 txt / md 猜成网页执行
            response.setHeader("X-Content-Type-Options", "nosniff");
            Files.copy(targetFile.toPath(), response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            // 多半是用户中途关了页面（连接已断），响应头已发出，这里只留痕
            System.out.println("文件下载失败：" + flag);
        }
    }

    /**
     * 删除文件
     */
    @RequirePermission("file:delete")
    @DeleteMapping("/{flag}")
    public void delFile(@PathVariable String flag) {
        File targetFile = getSafeFile(flag);
        if (targetFile != null) {
            FileUtil.del(targetFile);
            System.out.println("删除文件" + flag + "成功");
        }
    }

    private File getSafeFile(String flag) {
        if (StrUtil.isBlank(flag)) {
            return null;
        }
        try {
            File baseDir = new File(filePath);
            File targetFile = new File(baseDir, flag);
            String basePath = baseDir.getCanonicalPath();
            String targetPath = targetFile.getCanonicalPath();
            if (targetPath.startsWith(basePath + File.separator)) {
                return targetFile;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
