package com.example.demo.dto;

import lombok.Data;

@Data
public class OrderStatsVO {
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer pendingOrders;
    private Integer thisMonthOrders;
}