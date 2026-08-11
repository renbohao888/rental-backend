package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 仪表盘-最近订单
 */
@Data
public class RecentOrderVO {
    private Long id;
    private String orderNo;
    private Long roomId;
    private String roomTitle;
    private String roomTitleSnapshot;
    private String roomCoverSnapshot;
    private Long tenantId;
    private String tenantName;
    private BigDecimal totalAmount;
    private BigDecimal deposit;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer status;
    private String statusText;
    private String alipayTradeNo;
    private String adminRemark;
    private LocalDateTime createTime;
}
