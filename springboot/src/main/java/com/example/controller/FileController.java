package com.example.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.example.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;
import java.net.URLEncoder;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/files")
public class FileController {

    // 文件上传存储路径
    private static final String filePath = System.getProperty("user.dir") + "/files/";          //获取当前根目录(manager)下的files

    @Value("${server.port:9090}")
    private String port;

    @Value("${ip:localhost}")
    private String ip;

    /**
     * 文件上传
     */
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
        String http = "http://" + ip + ":" + port + "/files/";
        return Result.success(http + storedName);
    }


    /**
     * 获取文件
     *
     * @param flag
     * @param response
     */
    @GetMapping("/{flag}")   //  1697438073596-avatar.png
    public void avatarPath(@PathVariable String flag, HttpServletResponse response) {
        OutputStream os;
        try {
            File targetFile = getSafeFile(flag);
            if (targetFile != null && targetFile.exists()) {
                response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(flag, "UTF-8"));
                response.setContentType("application/octet-stream");
                byte[] bytes = FileUtil.readBytes(targetFile);
                os = response.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
            }
        } catch (Exception e) {
            System.out.println("文件下载失败");
        }
    }

    /**
     * 删除文件
     *
     * @param flag
     */
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
