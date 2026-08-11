package com.example.demo.dto;

import lombok.Data;

@Data
public class RepairUpdateDTO {
    private Long repairId;
    private Integer status; // 1-处理中，2-已完成，3-已关闭
    private String handlerRemark;
}