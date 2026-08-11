package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.LandlordApplicationVO;
import com.example.demo.service.LandlordApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/landlord")
public class LandlordApplicationController {

    @Autowired
    private LandlordApplicationService landlordApplicationService;

    /**
     * 租客提交房东入驻申请
     */
    @PostMapping("/apply")
    @RequiresRoles({2})
    public Result<String> submitApplication(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = landlordApplicationService.submitApplication(
                    userId,
                    params.get("realName"),
                    params.get("idCard"),
                    params.get("phone"),
                    params.get("remark"),
                    params.get("idCardFront"),
                    params.get("idCardBack"),
                    params.get("businessLicense")
            );
            return success ? Result.success("申请已提交") : Result.fail("提交失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 租客查询自己的申请状态
     */
    @GetMapping("/application/my")
    @RequiresRoles({2})
    public Result<LandlordApplicationVO> getMyApplication(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        LandlordApplicationVO vo = landlordApplicationService.getMyApplication(userId);
        return Result.success(vo);
    }

    /**
     * 管理员分页查询所有申请
     */
    @GetMapping("/applications")
    @RequiresRoles({0})
    public Result<IPage<LandlordApplicationVO>> getAllApplications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        IPage<LandlordApplicationVO> page = landlordApplicationService.getAllApplications(pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 管理员审核申请
     */
    @PostMapping("/audit/{applicationId}")
    @RequiresRoles({0})
    public Result<String> auditApplication(
            @PathVariable Long applicationId,
            @RequestParam Integer status,
            @RequestParam(required = false, defaultValue = "") String auditRemark) {
        try {
            boolean success = landlordApplicationService.auditApplication(applicationId, status, auditRemark);
            return success ? Result.success("审核完成") : Result.fail("审核失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 管理员查看申请详情
     */
    @GetMapping("/application/{applicationId}")
    @RequiresRoles({0})
    public Result<LandlordApplicationVO> getApplicationDetail(@PathVariable Long applicationId) {
        LandlordApplicationVO vo = landlordApplicationService.getApplicationDetail(applicationId);
        return Result.success(vo);
    }

    /**
     * 管理员撤销已通过的房东认证
     * 撤销后：用户降级为租客，该房东名下所有房源强制下架
     * @param applicationId 申请记录ID
     * @param reason 撤销原因（必填）
     */
    @PostMapping("/revoke/{applicationId}")
    @RequiresRoles({0})
    public Result<String> revokeApplication(
            @PathVariable Long applicationId,
            @RequestParam String reason) {
        try {
            boolean success = landlordApplicationService.revokeApplication(applicationId, reason);
            return success ? Result.success("已撤销认证，该房东名下房源已全部下架") : Result.fail("撤销失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}