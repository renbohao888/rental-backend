package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.DisputeHandleDTO;
import com.example.demo.dto.DisputeVO;
import com.example.demo.dto.TenantDisputeSubmitDTO;
import com.example.demo.service.DisputeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/dispute")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    /**
     * 租客查询自己的纠纷列表
     */
    @GetMapping("/list")
    @RequiresRoles({1, 2})
    public Result<List<DisputeVO>> getMyDisputes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<DisputeVO> page = disputeService.getMyDisputes(userId, pageNum, pageSize, status);
        return Result.success(page.getRecords());
    }

    /**
     * 租客发起纠纷
     */
    @PostMapping("/add")
    @RequiresRoles({1, 2})
    public Result<String> addDispute(HttpServletRequest request, @RequestBody TenantDisputeSubmitDTO dto) {
        Long userId = (Long) request.getAttribute("realUserId");
        if (dto.getOrderId() == null) {
            return Result.fail("订单不能为空");
        }
        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            return Result.fail("纠纷类型不能为空");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            return Result.fail("问题描述不能为空");
        }
        try {
            boolean success = disputeService.submitDispute(
                    userId,
                    dto.getOrderId(),
                    dto.getType(),
                    dto.getDescription(),
                    null);
            return success ? Result.success("纠纷申请已提交") : Result.fail("提交失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 管理员端接口 ==================

    /**
     * 管理员：分页查询所有纠纷
     */
    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public Result<IPage<DisputeVO>> getAdminDisputes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(disputeService.getAllDisputes(pageNum, pageSize, status));
    }

    /**
     * 管理员：处理纠纷（状态 1-处理中，2-已解决，3-已驳回）
     */
    @PutMapping("/handle")
    @RequiresRoles({0})
    public Result<String> handleDispute(@RequestBody DisputeHandleDTO dto) {
        if (dto.getDisputeId() == null) {
            return Result.fail("纠纷ID不能为空");
        }
        if (dto.getStatus() == null) {
            return Result.fail("处理状态不能为空");
        }
        boolean success = disputeService.handleDispute(
                dto.getDisputeId(),
                dto.getStatus(),
                dto.getAdminRemark(),
                dto.getResolution());
        return success ? Result.success("纠纷处理成功") : Result.fail("纠纷处理失败");
    }

    /**
     * 管理员：查询纠纷详情
     */
    @GetMapping("/detail/{disputeId}")
    @RequiresRoles({0, 1, 2})
    public Result<DisputeVO> getDisputeDetail(@PathVariable Long disputeId) {
        DisputeVO vo = disputeService.getDisputeDetail(disputeId);
        if (vo == null) {
            return Result.fail("纠纷不存在");
        }
        return Result.success(vo);
    }
}
