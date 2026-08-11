package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RevenueStatsVO {
    private BigDecimal totalRevenue;
    private BigDecimal thisMonthRevenue;
    private BigDecimal avgOrderAmount;
}