package com.example.demo.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class FileUploadUtils {

    @Value("${file.upload.path:uploads/}")
    private String baseUploadPath;

    /**
     * 上传多个图片文件
     * @param files 图片文件列表
     * @param subDir 子目录（如 repair）
     * @return 图片访问URL列表
     */
    public List<String> uploadImages(List<MultipartFile> files, String subDir) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            String url = uploadImage(file, subDir);
            if (url != null) {
                urls.add(url);
            }
        }
        return urls;
    }

    /**
     * 上传单张图片
     */
    public String uploadImage(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            return null;
        }

        // 1. 校验文件类型（仅图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("仅支持图片文件上传");
        }

        try {
            // 3. 构建存储路径： uploads/subDir/yyyy-MM/
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String uploadDir = baseUploadPath + subDir + "/" + datePath + "/";

            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 4. 生成唯一文件名：时间戳_UUID.扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // 5. 保存文件
            File destFile = new File(dir, newFileName);
            file.transferTo(destFile);

            // 6. 返回访问URL（相对路径，前端通过 /uploads/ 访问）
            return "/uploads/" + subDir + "/" + datePath + "/" + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 保存 base64 图片（data URL 格式），用于注册页无需登录即可上传头像
     * @param dataUrl 形如 data:image/png;base64,xxxx
     * @param subDir 子目录（如 avatar）
     * @return 图片访问URL
     */
    public String saveBase64Image(String dataUrl, String subDir) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/")) {
            throw new RuntimeException("图片格式不正确，仅支持图片上传");
        }

        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex <= 0) {
            throw new RuntimeException("图片数据格式不正确");
        }

        String meta = dataUrl.substring(0, commaIndex);
        String base64Data = dataUrl.substring(commaIndex + 1);

        // 从 data:image/png 中提取 MIME 类型并映射扩展名
        String mime = meta.contains(";") ? meta.substring(5, meta.indexOf(';')) : meta.substring(5);
        String extension = switch (mime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            default -> ".png";
        };

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("图片数据无法解析");
        }
        if (bytes.length == 0) {
            throw new RuntimeException("图片数据为空");
        }

        // 构建存储路径： uploads/subDir/yyyy-MM/
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String uploadDir = baseUploadPath + subDir + "/" + datePath + "/";

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String newFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        File destFile = new File(dir, newFileName);
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException("图片保存失败：" + e.getMessage());
        }
        return "/uploads/" + subDir + "/" + datePath + "/" + newFileName;
    }
}