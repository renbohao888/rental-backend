package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BusinessStatsVO {
    // ===== 房源概览 =====
    private Integer totalRooms;
    private Integer publishedRooms;
    private Integer rentedRooms;
    private Integer pendingAuditRooms;

    // ===== 订单概览 =====
    private Integer totalOrders;
    private Integer thisMonthOrders;
    private Integer pendingOrders; // 待处理订单（待支付 + 待入住）

    // ===== 收入概览 =====
    private BigDecimal totalRevenue;
    private BigDecimal thisMonthRevenue;
    private BigDecimal avgOrderAmount;

    // ===== 趋势数据（近7天） =====
    private List<DailyTrendVO> dailyTrend;
}