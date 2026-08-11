package com.example.demo.dto;

import lombok.Data;

@Data
public class DisputeHandleDTO {
    private Long disputeId;
    private Integer status; // 1-处理中，2-已解决，3-已驳回
    private String adminRemark;
    private String resolution;
}