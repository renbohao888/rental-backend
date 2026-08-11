package com.example.demo.dto;

import com.example.demo.entity.Repair;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房东仪表盘统计数据
 */
@Data
public class LandlordDashboardVO {

    // ===== 房源概览 =====
    private Integer totalRooms;        // 房源总数
    private Integer publishedRooms;    // 已上架
    private Integer rentedRooms;       // 已租出
    private Integer pendingAuditRooms; // 待审核
    private Integer rejectedRooms;     // 被驳回

    // ===== 订单概览 =====
    private Integer totalOrders;       // 总订单
    private Integer activeOrders;      // 进行中订单（已支付待入住/已入住/退租核算中）
    private Integer thisMonthOrders;   // 本月订单

    // ===== 收入概览 =====
    private BigDecimal totalRevenue;     // 总收入（已完成）
    private BigDecimal thisMonthRevenue; // 本月收入（已完成）
    private BigDecimal avgOrderAmount;   // 平均客单价

    // ===== 评价 =====
    private BigDecimal avgRating;      // 房源平均评分

    // ===== 报修概览 =====
    private Integer totalRepairs;        // 报修总数
    private Integer pendingRepairs;      // 待处理报修
    private Integer processingRepairs;   // 处理中报修

    // ===== 最近订单 =====
    private List<RecentOrderVO> recentOrders;

    // ===== 待处理报修列表（待处理+处理中） =====
    private List<Repair> pendingRepairList;
}
