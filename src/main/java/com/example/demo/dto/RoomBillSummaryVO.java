package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 房源账单汇总（按房源统计）
 */
@Data
public class RoomBillSummaryVO {
    /**
     * 房源ID
     */
    private Long roomId;

    /**
     * 房源标题
     */
    private String roomTitle;

    /**
     * 该房源总订单数（含所有有效状态，不含已取消和已拒绝）
     */
    private Integer totalOrders;

    /**
     * 已完成订单数（status=4）
     */
    private Integer completedOrders;

    /**
     * 租金总收入（仅统计已完成订单 status=4）
     */
    private BigDecimal totalRevenue;

    /**
     * 押金总额（仅统计已完成订单 status=4）
     */
    private BigDecimal totalDeposit;
}