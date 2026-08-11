package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.AppointmentHandleDTO;
import com.example.demo.dto.AppointmentSubmitDTO;
import com.example.demo.dto.AppointmentVO;
import com.example.demo.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@CrossOrigin
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // ================== 租客端接口 ==================

    /**
     * 租客提交看房预约
     */
    @PostMapping("/submit")
    @RequiresRoles({1, 2})
    public Result<String> submitAppointment(HttpServletRequest request, @RequestBody AppointmentSubmitDTO dto) {
        Long userId = (Long) request.getAttribute("realUserId");

        if (dto.getRoomId() == null) {
            return Result.fail("房源ID不能为空");
        }
        if (dto.getAppointmentDate() == null) {
            return Result.fail("预约日期不能为空");
        }
        if (dto.getAppointmentTime() == null || dto.getAppointmentTime().trim().isEmpty()) {
            return Result.fail("预约时间段不能为空");
        }

        try {
            boolean success = appointmentService.submitAppointment(
                    userId,
                    dto.getRoomId(),
                    dto.getAppointmentDate(),
                    dto.getAppointmentTime(),
                    dto.getRemark()
            );
            return success ? Result.success("预约提交成功，等待房东确认") : Result.fail("预约提交失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 租客查询自己的预约列表
     */
    @GetMapping("/my")
    @RequiresRoles({1, 2})
    public Result<IPage<AppointmentVO>> getMyAppointments(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<AppointmentVO> page = appointmentService.getMyAppointments(userId, pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 租客标记已看房（确认看房完成后）
     */
    @PostMapping("/view/{appointmentId}")
    @RequiresRoles({1, 2})
    public Result<String> markAsViewed(HttpServletRequest request, @PathVariable Long appointmentId) {
        Long userId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = appointmentService.markAsViewed(appointmentId, userId);
            return success ? Result.success("已标记看房完成") : Result.fail("操作失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 租客取消预约（仅待确认状态，本人操作）
     */
    @PostMapping("/cancel/{appointmentId}")
    @RequiresRoles({1, 2})
    public Result<String> cancelAppointment(HttpServletRequest request, @PathVariable Long appointmentId) {
        Long userId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = appointmentService.cancelAppointment(appointmentId, userId);
            return success ? Result.success("预约已取消") : Result.fail("取消失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 房东端接口 ==================

    /**
     * 房东查询自己房源的预约列表
     */
    @GetMapping("/landlord/list")
    @RequiresRoles({1})
    public Result<IPage<AppointmentVO>> getLandlordAppointments(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        IPage<AppointmentVO> page = appointmentService.getLandlordAppointments(landlordId, pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 房东处理预约（确认/拒绝）
     */
    @PutMapping("/handle")
    @RequiresRoles({1})
    public Result<String> handleAppointment(HttpServletRequest request, @RequestBody AppointmentHandleDTO dto) {
        Long landlordId = (Long) request.getAttribute("realUserId");

        if (dto.getAppointmentId() == null) {
            return Result.fail("预约ID不能为空");
        }
        if (dto.getStatus() == null || (dto.getStatus() != 1 && dto.getStatus() != 2)) {
            return Result.fail("状态参数错误，仅支持1-确认、2-拒绝");
        }

        try {
            boolean success = appointmentService.handleAppointment(
                    dto.getAppointmentId(),
                    landlordId,
                    dto.getStatus(),
                    dto.getLandlordRemark()
            );
            return success ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 公共接口（登录用户都可查看详情） ==================

    /**
     * 查询预约详情（租客、房东、管理员均可查看）
     */
    @GetMapping("/{appointmentId}")
    @RequiresRoles({0, 1, 2})
    public Result<AppointmentVO> getAppointmentDetail(@PathVariable Long appointmentId) {
        AppointmentVO vo = appointmentService.getAppointmentDetail(appointmentId);
        if (vo == null) {
            return Result.fail("预约不存在");
        }
        return Result.success(vo);
    }

    // ================== 管理员端接口 ==================

    /**
     * 管理员查询所有预约列表
     */
    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public Result<IPage<AppointmentVO>> getAllAppointments(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        IPage<AppointmentVO> page = appointmentService.getAllAppointments(pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 管理员删除预约
     */
    @DeleteMapping("/admin/{appointmentId}")
    @RequiresRoles({0})
    public Result<String> deleteAppointment(@PathVariable Long appointmentId) {
        boolean success = appointmentService.deleteAppointment(appointmentId);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }
}