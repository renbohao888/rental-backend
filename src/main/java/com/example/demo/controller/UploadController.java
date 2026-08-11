package com.example.demo.controller;

import com.example.demo.common.FileUploadUtils;
import com.example.demo.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired
    private FileUploadUtils fileUploadUtils;

    /**
     * 通用单图片上传（供前端 el-upload action="/api/upload" 使用）
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            List<String> urls = fileUploadUtils.uploadImages(List.of(file), "common");
            if (urls.isEmpty()) {
                return Result.fail("上传失败");
            }
            return Result.success(urls.get(0));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 图片上传（供前端 el-upload action="/api/upload/image" 使用，如身份证、营业执照等）
     */
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            List<String> urls = fileUploadUtils.uploadImages(List.of(file), "image");
            if (urls.isEmpty()) {
                return Result.fail("上传失败");
            }
            return Result.success(urls.get(0));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 报修图片上传（供前端 tenant/Repairs.vue 的 /api/repair/upload 使用）
     */
    @PostMapping("/repair/upload")
    public Result<String> uploadRepairImage(@RequestParam("file") MultipartFile file) {
        try {
            List<String> urls = fileUploadUtils.uploadImages(List.of(file), "repair");
            if (urls.isEmpty()) {
                return Result.fail("上传失败");
            }
            return Result.success(urls.get(0));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
