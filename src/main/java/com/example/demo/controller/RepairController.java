package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.FileUploadUtils;
import com.example.demo.common.Result;
import com.example.demo.dto.DisputeVO;
import com.example.demo.dto.RepairSubmitDTO;
import com.example.demo.dto.RepairUpdateDTO;
import com.example.demo.dto.SupervisionStatsVO;
import com.example.demo.entity.Repair;
import com.example.demo.service.DisputeService;
import com.example.demo.service.RepairService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/repair")
public class RepairController {

    @Autowired
    private RepairService repairService;

    @Autowired
    private DisputeService disputeService;  // 👈 新增注入

    @Autowired
    private FileUploadUtils fileUploadUtils;

    // ================== 租客端接口 ==================

    /**
     * 租客提交报修（含图片上传）
     */
    @PostMapping("/submit")
    @RequiresRoles({1, 2})
    public Result<String> submitRepair(
            HttpServletRequest request,
            @RequestPart("repair") RepairSubmitDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        Long userId = (Long) request.getAttribute("realUserId");

        if (dto.getRoomId() == null) {
            return Result.fail("房源ID不能为空");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return Result.fail("报修标题不能为空");
        }

        List<String> imageUrls = null;
        if (files != null && !files.isEmpty()) {
            try {
                imageUrls = fileUploadUtils.uploadImages(files, "repair");
            } catch (RuntimeException e) {
                return Result.fail(e.getMessage());
            }
        }

        try {
            boolean success = repairService.submitRepair(
                    userId,
                    dto.getRoomId(),
                    dto.getTitle(),
                    dto.getDescription(),
                    imageUrls
            );
            return success ? Result.success("报修提交成功") : Result.fail("报修提交失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 租客撤销报修（仅待处理状态 status=0 可撤销）
     */
    @PostMapping("/cancel/{repairId}")
    @RequiresRoles({1, 2})
    public Result<String> cancelRepair(HttpServletRequest request, @PathVariable Long repairId) {
        Long userId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = repairService.cancelRepair(userId, repairId);
            return success ? Result.success("已撤销报修") : Result.fail("撤销失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 租客查询自己报修 ==================

    /**
     * 租客查询自己的报修列表
     */
    @GetMapping("/my")
    @RequiresRoles({1, 2})
    public Result<IPage<Repair>> getMyRepairs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<Repair> page = repairService.getMyRepairs(userId, pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 租客查询自己的报修列表（数组形式，对齐前端 my/Repairs.vue）
     */
    @GetMapping("/list")
    @RequiresRoles({2})
    public Result<List<Repair>> getMyRepairList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<Repair> page = repairService.getMyRepairs(userId, 1, 100, null);
        return Result.success(page.getRecords());
    }

    /**
     * 租客提交报修（JSON 表单，对齐前端 my/Repairs.vue）
     */
    @PostMapping("/add")
    @RequiresRoles({2})
    public Result<String> addRepair(HttpServletRequest request, @RequestBody RepairSubmitDTO dto) {
        Long userId = (Long) request.getAttribute("realUserId");
        if (dto.getRoomId() == null) {
            return Result.fail("房源ID不能为空");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return Result.fail("报修标题不能为空");
        }
        try {
            // 前端 images 为逗号分隔字符串，转为 List 传给服务层
            List<String> imageUrls = null;
            if (dto.getImages() != null && !dto.getImages().isBlank()) {
                imageUrls = Arrays.stream(dto.getImages().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            boolean success = repairService.submitRepair(
                    userId,
                    dto.getRoomId(),
                    dto.getTitle(),
                    dto.getDescription(),
                    imageUrls);
            return success ? Result.success("报修提交成功") : Result.fail("报修提交失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 房东端接口 ==================

    @GetMapping("/landlord/list")
    @RequiresRoles({1})
    public Result<IPage<Repair>> getLandlordRepairs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        IPage<Repair> page = repairService.getLandlordRepairs(landlordId, pageNum, pageSize, status);
        return Result.success(page);
    }

    // ================== 公共接口（登录用户都可查看详情） ==================

    @GetMapping("/{repairId}")
    @RequiresRoles({0, 1, 2})
    public Result<Repair> getRepairDetail(@PathVariable Long repairId) {
        Repair repair = repairService.getRepairDetail(repairId);
        if (repair == null) {
            return Result.fail("报修记录不存在");
        }
        return Result.success(repair);
    }

    // ================== 处理人接口（房东/管理员） ==================

    @PutMapping("/handle")
    @RequiresRoles({0, 1})
    public Result<String> handleRepair(HttpServletRequest request, @RequestBody RepairUpdateDTO dto) {
        Long handlerId = (Long) request.getAttribute("realUserId");

        if (dto.getRepairId() == null) {
            return Result.fail("报修ID不能为空");
        }
        if (dto.getStatus() == null) {
            return Result.fail("处理状态不能为空");
        }

        try {
            boolean success = repairService.handleRepair(
                    dto.getRepairId(),
                    handlerId,
                    dto.getStatus(),
                    dto.getHandlerRemark()
            );
            return success ? Result.success("处理成功") : Result.fail("处理失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 管理员端接口 ==================

    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public Result<IPage<Repair>> getAllRepairs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        IPage<Repair> page = repairService.getAllRepairs(pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 管理员获取报修投诉督办统计数据
     */
    @GetMapping("/admin/supervision/stats")
    @RequiresRoles({0})
    public Result<SupervisionStatsVO> getSupervisionStats() {
        SupervisionStatsVO stats = repairService.getSupervisionStats();
        return Result.success(stats);
    }

    /**
     * 管理员获取报修投诉督办列表（统一入口）
     * type: repair-报修，dispute-纠纷
     */
    @GetMapping("/admin/supervision/list")
    @RequiresRoles({0})
    public Result<Map<String, Object>> getSupervisionList(
            @RequestParam String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> result = new HashMap<>();

        if ("repair".equalsIgnoreCase(type)) {
            IPage<Repair> page = repairService.getAllRepairs(pageNum, pageSize, status);
            result.put("type", "repair");
            result.put("list", page);
        } else if ("dispute".equalsIgnoreCase(type)) {
            IPage<DisputeVO> page = disputeService.getAllDisputes(pageNum, pageSize, status);
            result.put("type", "dispute");
            result.put("list", page);
        } else {
            return Result.fail("type参数错误，仅支持 repair 或 dispute");
        }

        return Result.success(result);
    }
}