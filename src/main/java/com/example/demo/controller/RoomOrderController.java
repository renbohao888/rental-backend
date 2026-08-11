package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.BusinessStatsVO;
import com.example.demo.dto.OrderCreateDTO;
import com.example.demo.entity.RoomOrder;
import com.example.demo.service.RoomOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/order")
public class RoomOrderController {

    @Autowired
    private RoomOrderService roomOrderService;

    /**
     * 1. 租客下单（只有租客 2 可以调用）
     */
    @PostMapping("/create")
    @RequiresRoles({1, 2})
    public Result<RoomOrder> createOrder(HttpServletRequest request, @RequestBody OrderCreateDTO dto) {
        Long userId = (Long) request.getAttribute("realUserId");
        RoomOrder order = roomOrderService.createOrder(userId, dto.getRoomId(), dto.getCheckInDate(), dto.getCheckOutDate());
        return Result.success(order);
    }

    /**
     * 2. 获取我的订单（只有租客 2 可以调用）
     */
    @GetMapping("/my")
    @RequiresRoles({1, 2})
    public Result<List<RoomOrder>> getMyOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        return Result.success(roomOrderService.getUserOrders(userId));
    }

    /**
     * 2.1 租客端我的订单分页列表（对齐前端 my/Orders.vue 与 my/Disputes.vue）
     */
    @GetMapping("/tenant/list")
    @RequiresRoles({0, 1, 2})
    public Result<IPage<RoomOrder>> getTenantOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<RoomOrder> page = roomOrderService.getUserOrdersPage(userId, pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 2.2 租客取消订单（待支付状态）
     */
    @PostMapping("/{orderId}/cancel")
    @RequiresRoles({1, 2})
    public Result<String> cancelOrder(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = roomOrderService.cancelOrder(orderId, userId);
            return success ? Result.success("订单已取消") : Result.fail("取消失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ================== 原有删除接口保留 ==================

    /**
     * 3. 删除单个订单（只有租客 2 可以调用）
     */
    @DeleteMapping("/{orderId}")
    @RequiresRoles({1, 2})
    public String deleteOrder(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("realUserId");
        boolean success = roomOrderService.deleteOrder(orderId, userId);
        return success ? "删除成功" : "删除失败，订单不存在或无权限";
    }

    /**
     * 4. 批量删除订单（只有租客 2 可以调用）
     */
    @DeleteMapping("/batch")
    @RequiresRoles({1, 2})
    public String batchDeleteOrders(HttpServletRequest request, @RequestBody List<Long> orderIds) {
        Long userId = (Long) request.getAttribute("realUserId");
        int count = roomOrderService.batchDeleteOrders(orderIds, userId);
        return "成功删除了 " + count + " 条订单";
    }

    /**
     * 5. 强制物理删除订单（只有管理员 0 可以调用）
     */
    @DeleteMapping("/force/{orderId}")
    @RequiresRoles({0})
    public String forceDeleteOrder(@PathVariable Long orderId) {
        boolean success = roomOrderService.forceDeleteOrder(orderId);
        return success ? "强制物理删除成功" : "删除失败";
    }

    // ================== 新增：退租闭环接口 ==================

    /**
     * 确认入住（已支付待入住 → 已入住）
     * 租客本人、房东、管理员均可确认
     */
    @PostMapping("/checkin/{orderId}")
    @RequiresRoles({0, 1, 2})
    public String checkIn(HttpServletRequest request, @PathVariable Long orderId) {
        Long currentUserId = (Long) request.getAttribute("realUserId");
        Integer role = (Integer) request.getAttribute("userRole");
        boolean success = roomOrderService.checkIn(orderId, currentUserId, role);
        return success ? "确认入住成功" : "确认入住失败";
    }

    /**
     * 租客申请退房
     */
    @PostMapping("/checkout/{orderId}")
    @RequiresRoles({1, 2})
    public String applyCheckOut(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("realUserId");
        boolean success = roomOrderService.applyCheckOut(orderId, userId);
        return success ? "退房申请已提交，等待房东结算" : "退房申请失败";
    }

    /**
     * 房东/管理员确认结算、完结订单
     */
    @PostMapping("/complete/{orderId}")
    @RequiresRoles({0, 1})
    public String completeOrder(HttpServletRequest request, @PathVariable Long orderId, @RequestParam BigDecimal deductAmount) {
        Long currentUserId = (Long) request.getAttribute("realUserId");
        // 仅修改此处key：拦截器存的是userRole，不是role，其余逻辑完全不动
        Integer role = (Integer) request.getAttribute("userRole");

        // 传入操作人ID，方法内部会做校验
        boolean success = roomOrderService.completeOrder(orderId, currentUserId, role, deductAmount);
        return success ? "订单结算完成，房源已释放" : "订单结算失败，权限不足或状态异常";
    }

    // ================== 新增：撤销+拒单接口 ==================

    /**
     * 租客撤销订单（房东处理前可撤销）
     */
    @PostMapping("/cancel/{orderId}")
    @RequiresRoles({1, 2})
    public String revokeOrder(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("realUserId");
        boolean success = roomOrderService.cancelOrder(orderId, userId);
        return success ? "订单已撤销" : "订单撤销失败";
    }

    /**
     * 房东拒单（不受理租房申请）
     */
    @PostMapping("/reject/{orderId}")
    @RequiresRoles({1})
    public String rejectOrder(HttpServletRequest request, @PathVariable Long orderId) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        boolean success = roomOrderService.rejectOrder(orderId, landlordId);
        return success ? "订单已拒绝" : "订单拒单失败";
    }

    // ================== 新增：后台管理订单接口 ==================

    /**
     * 管理员分页查询所有订单
     */
    @GetMapping("/admin/all")
    @RequiresRoles({0})
    public IPage<RoomOrder> getAllOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return roomOrderService.getAllOrdersPage(pageNum, pageSize, status);
    }

    /**
     * 房东查看自己房源的所有订单
     */
    @GetMapping("/landlord/my")
    @RequiresRoles({1})
    public List<RoomOrder> getLandlordOrders(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        return roomOrderService.getLandlordOrders(landlordId);
    }

    /**
     * 房东删除自己房源下的订单
     */
    @DeleteMapping("/landlord/{orderId}")
    @RequiresRoles({1})
    public Result<String> deleteLandlordOrder(HttpServletRequest request, @PathVariable Long orderId) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        boolean success = roomOrderService.deleteLandlordOrder(orderId, landlordId);
        return success ? Result.success("删除成功") : Result.fail("删除失败，订单不存在或无权限");
    }

    /**
     * 房东批量删除自己房源下的订单
     */
    @DeleteMapping("/landlord/batch")
    @RequiresRoles({1})
    public Result<String> batchDeleteLandlordOrders(HttpServletRequest request, @RequestBody List<Long> orderIds) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        int count = roomOrderService.batchDeleteLandlordOrders(orderIds, landlordId);
        return Result.success("成功删除了 " + count + " 条订单");
    }

    /**
     * 房东获取经营统计数据
     * 仅房东可调用
     */
    @GetMapping("/stats")
    @RequiresRoles({1})
    public Result<BusinessStatsVO> getBusinessStats(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        BusinessStatsVO stats = roomOrderService.getBusinessStats(landlordId);
        return Result.success(stats);
    }

    /**
     * 管理员强制退款
     */
    @PostMapping("/admin/force-refund/{orderId}")
    @RequiresRoles({0})
    public Result<String> adminForceRefund(
            HttpServletRequest request,
            @PathVariable Long orderId,
            @RequestParam String reason) {
        Long adminId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = roomOrderService.adminForceRefund(orderId, adminId, reason);
            return success ? Result.success("强制退款成功") : Result.fail("强制退款失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 管理员强制退房
     */
    @PostMapping("/admin/force-checkout/{orderId}")
    @RequiresRoles({0})
    public Result<String> adminForceCheckout(
            HttpServletRequest request,
            @PathVariable Long orderId,
            @RequestParam String reason) {
        Long adminId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = roomOrderService.adminForceCheckout(orderId, adminId, reason);
            return success ? Result.success("强制退房成功") : Result.fail("强制退房失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 管理员物理删除订单（不可恢复）
     */
    @DeleteMapping("/admin/force-delete/{orderId}")
    @RequiresRoles({0})
    public Result<String> adminForceDeleteOrder(@PathVariable Long orderId) {
        boolean success = roomOrderService.forceDeleteOrder(orderId);
        return success ? Result.success("删除成功") : Result.fail("删除失败，订单不存在");
    }

    /**
     * 管理员批量物理删除订单（不可恢复）
     */
    @DeleteMapping("/admin/force-delete/batch")
    @RequiresRoles({0})
    public Result<String> adminForceBatchDeleteOrders(@RequestBody List<Long> orderIds) {
        int count = 0;
        if (orderIds != null && !orderIds.isEmpty()) {
            for (Long orderId : orderIds) {
                if (roomOrderService.forceDeleteOrder(orderId)) {
                    count++;
                }
            }
        }
        return Result.success("成功删除了 " + count + " 条订单");
    }

    /**
     * 管理员强制撤销房源
     */
    @PostMapping("/admin/force-remove-room/{roomId}")
    @RequiresRoles({0})
    public Result<String> adminForceRemoveRoom(
            HttpServletRequest request,
            @PathVariable Long roomId,
            @RequestParam String reason) {
        Long adminId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = roomOrderService.adminForceRemoveRoom(roomId, adminId, reason);
            return success ? Result.success("房源强制下架成功") : Result.fail("房源强制下架失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}