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

/**
 * 文件接口
 */
@RestController
@RequestMapping("/files")
public class FileController {

    // 文件上传存储路径
    private static final String filePath = System.getProperty("user.dir") + "/files/";

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
        String storedName;
        try {
            byte[] bytes = file.getBytes();
            String md5 = SecureUtil.md5().digestHex(bytes);
            storedName = StrUtil.isBlank(suffix) ? md5 : md5 + "." + suffix.toLowerCase();
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
     * 获取文件
     */
    @GetMapping("/{flag}")
    public void avatarPath(@PathVariable String flag, HttpServletResponse response) {
        try {
            File targetFile = getSafeFile(flag);
            if (targetFile != null && targetFile.exists()) {
                // 必须按真实扩展名返回图片 MIME（image/png 等）：
                // 微信真机的原生图片组件只渲染 image/*，收到 application/octet-stream 会拒绝显示（空白）；
                // 开发者工具模拟器是 Chromium 内核会内容嗅探，所以模拟器正常、真机空白——不能以模拟器表现为准。
                // 同时不能带 Content-Disposition: attachment（附件语义，真机同样不渲染）。
                MediaType mediaType = MediaTypeFactory.getMediaType(flag).orElse(MediaType.APPLICATION_OCTET_STREAM);
                byte[] bytes = FileUtil.readBytes(targetFile);
                response.setContentType(mediaType.toString());
                response.setContentLength(bytes.length);
                response.getOutputStream().write(bytes);
                response.getOutputStream().flush();
            }
        } catch (Exception e) {
            System.out.println("文件下载失败");
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
