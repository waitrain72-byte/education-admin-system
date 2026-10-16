package com.example.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 文件下载：按扩展名给出真实类型、带长期缓存头（文件名就是内容 MD5，内容不会变），
 * 找不到或想跳出存储目录时返回 404。存储目录换成临时目录，不碰项目里的 files/。
 */
class FileControllerTest {

    /** 临时目录：files/ 是存储目录，secret.txt 放在它外面 */
    @TempDir
    Path root;

    private FileController controller;

    @BeforeEach
    void setUp() throws Exception {
        Path storage = Files.createDirectories(root.resolve("files"));
        controller = new FileController();
        ReflectionTestUtils.setField(controller, "filePath", storage.toString() + File.separator);
        Files.write(storage.resolve("0cc175b9c0f1b6a831c399e269772661.png"), new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        Files.write(root.resolve("secret.txt"), "top secret".getBytes("UTF-8"));
    }

    @Test
    @DisplayName("图片：image/png、长度、长期缓存、禁止类型嗅探，内容原样返回")
    void servesImageWithCacheHeaders() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.avatarPath("0cc175b9c0f1b6a831c399e269772661.png", response);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals(4, response.getContentLengthLong());
        assertEquals(FileController.CACHE_CONTROL, response.getHeader("Cache-Control"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertNull(response.getHeader("Content-Disposition"));
        assertArrayEquals(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, response.getContentAsByteArray());
    }

    @Test
    @DisplayName("文件不存在：404，不带缓存头（以后传上来了还能取到）")
    void missingFileIs404() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.avatarPath("ffffffffffffffffffffffffffffffff.png", response);

        assertEquals(404, response.getStatus());
        assertNull(response.getHeader("Cache-Control"));
        assertEquals(0, response.getContentAsByteArray().length);
    }

    @Test
    @DisplayName("想用 ../ 跳出存储目录读别的文件：404")
    void pathTraversalIs404() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.avatarPath("../secret.txt", response);

        assertEquals(404, response.getStatus());
        assertEquals(0, response.getContentAsByteArray().length);
    }
}
