package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class HotRoomVO {
    private Long roomId;
    private String roomTitle;
    private String roomCover;
    private Integer orderCount;
    private BigDecimal totalRevenue;
}