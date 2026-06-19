package com.shop.controller;

import com.shop.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 文件上传接口
 */
@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    @Value("${upload.path:/tmp/uploads}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(Authentication auth, @RequestParam("file") MultipartFile file) {
        Long userId = requireUserId(auth);

        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.fail("文件名无效");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return Result.fail("仅支持 " + ALLOWED_IMAGE_EXTENSIONS + " 格式的图片");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            return Result.fail("图片大小不能超过 5MB");
        }

        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path filePath = uploadDir.resolve(newFilename);
            file.transferTo(filePath.toFile());

            String fileUrl = urlPrefix + "/" + newFilename;
            log.info("Image uploaded: userId={}, url={}, size={}", userId, fileUrl, file.getSize());

            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("filename", newFilename);
            data.put("originalName", originalFilename);
            return Result.ok(data);
        } catch (IOException e) {
            log.error("Failed to upload image", e);
            return Result.fail("上传失败：" + e.getMessage());
        }
    }
}
