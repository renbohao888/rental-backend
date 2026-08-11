package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyTrendVO {
    private LocalDate date;
    private Integer orderCount;
    private BigDecimal revenue;
}