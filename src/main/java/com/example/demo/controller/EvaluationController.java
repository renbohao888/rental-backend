package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.FileUploadUtils;
import com.example.demo.common.Result;
import com.example.demo.dto.EvaluationReplyDTO;
import com.example.demo.dto.EvaluationSubmitDTO;
import com.example.demo.dto.EvaluationVO;
import com.example.demo.dto.TenantEvaluationAddDTO;
import com.example.demo.entity.Evaluation;
import com.example.demo.service.EvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private FileUploadUtils fileUploadUtils;

    // ================== 租客端接口 ==================

    /**
     * 租客提交评价（含图片上传）
     */
    @PostMapping("/submit")
    @RequiresRoles({1, 2})
    public Result<String> submitEvaluation(
            HttpServletRequest request,
            @RequestPart("evaluation") EvaluationSubmitDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        Long userId = (Long) request.getAttribute("realUserId");

        // 参数校验
        if (dto.getOrderId() == null) {
            return Result.fail("订单ID不能为空");
        }
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            return Result.fail("评分必须在1-5之间");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return Result.fail("评价内容不能为空");
        }

        // 上传图片
        List<String> imageUrls = null;
        if (files != null && !files.isEmpty()) {
            try {
                imageUrls = fileUploadUtils.uploadImages(files, "evaluation");
            } catch (RuntimeException e) {
                return Result.fail(e.getMessage());
            }
        }

        try {
            boolean success = evaluationService.submitEvaluation(
                    userId,
                    dto.getOrderId(),
                    dto.getRating(),
                    dto.getContent(),
                    imageUrls
            );
            return success ? Result.success("评价提交成功") : Result.fail("评价提交失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 租客查询自己的评价列表
     */
    @GetMapping("/my")
    @RequiresRoles({1, 2})
    public Result<IPage<EvaluationVO>> getMyEvaluations(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<EvaluationVO> page = evaluationService.getMyEvaluations(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 租客查询自己的评价列表（数组形式，对齐前端 my/Evaluations.vue）
     */
    @GetMapping("/list")
    @RequiresRoles({1, 2})
    public Result<List<EvaluationVO>> getMyEvaluationList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<EvaluationVO> page = evaluationService.getMyEvaluations(userId, 1, 100);
        return Result.success(page.getRecords());
    }

    /**
     * 租客发表评价（JSON 表单，对齐前端 my/Evaluations.vue）
     */
    @PostMapping("/add")
    @RequiresRoles({1, 2})
    public Result<String> addEvaluation(HttpServletRequest request, @RequestBody TenantEvaluationAddDTO dto) {
        Long userId = (Long) request.getAttribute("realUserId");
        if (dto.getOrderId() == null) {
            return Result.fail("请先选中需要评价的已完成订单");
        }
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            return Result.fail("评分必须在1-5之间");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return Result.fail("评价内容不能为空");
        }
        try {
            List<String> imageUrls = null;
            if (dto.getImages() != null && !dto.getImages().isBlank()) {
                imageUrls = Arrays.stream(dto.getImages().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            boolean success = evaluationService.submitEvaluation(
                    userId,
                    dto.getOrderId(),
                    dto.getRating(),
                    dto.getContent(),
                    imageUrls);
            return success ? Result.success("评价已发表") : Result.fail("发表失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 房东端接口 ==================

    /**
     * 房东查询自己房源的评价列表
     */
    @GetMapping("/landlord/list")
    @RequiresRoles({1})
    public Result<IPage<EvaluationVO>> getLandlordEvaluations(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        IPage<EvaluationVO> page = evaluationService.getLandlordEvaluations(landlordId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 房东回复评价
     */
    @PostMapping("/reply")
    @RequiresRoles({1})
    public Result<String> replyEvaluation(HttpServletRequest request, @RequestBody EvaluationReplyDTO dto) {
        Long landlordId = (Long) request.getAttribute("realUserId");

        if (dto.getEvaluationId() == null) {
            return Result.fail("评价ID不能为空");
        }
        if (dto.getReplyContent() == null || dto.getReplyContent().trim().isEmpty()) {
            return Result.fail("回复内容不能为空");
        }

        try {
            boolean success = evaluationService.replyEvaluation(
                    dto.getEvaluationId(),
                    landlordId,
                    dto.getReplyContent()
            );
            return success ? Result.success("回复成功") : Result.fail("回复失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 用户端公开接口 ==================

    /**
     * 查看房源的评价列表（公开，用于前端展示）
     */
    @GetMapping("/room/{roomId}")
    public Result<IPage<EvaluationVO>> getRoomEvaluations(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<EvaluationVO> page = evaluationService.getRoomEvaluations(roomId, pageNum, pageSize);
        return Result.success(page);
    }

    // ================== 管理员端接口 ==================

    /**
     * 管理员查询所有评价列表
     */
    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public Result<IPage<EvaluationVO>> getAllEvaluations(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<EvaluationVO> page = evaluationService.getAllEvaluations(pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 管理员删除评价
     */
    @DeleteMapping("/admin/{evaluationId}")
    @RequiresRoles({0})
    public Result<String> deleteEvaluation(@PathVariable Long evaluationId) {
        boolean success = evaluationService.deleteEvaluation(evaluationId);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }
}