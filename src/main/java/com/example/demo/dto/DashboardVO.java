package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    // ===== 用户概览 =====
    private UserStatsVO userStats;

    // ===== 房源概览 =====
    private RoomStatsVO roomStats;

    // ===== 订单概览 =====
    private OrderStatsVO orderStats;

    // ===== 收入概览 =====
    private RevenueStatsVO revenueStats;

    // ===== 近7天趋势 =====
    private List<DailyTrendVO> recentTrend;

    // ===== 热门房源Top5 =====
    private List<HotRoomVO> hotRooms;
}