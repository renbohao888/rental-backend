package com.example.demo.dto;

import lombok.Data;

@Data
public class LandlordAuditDTO {
    private Long applicationId;
    private Integer status; // 1-通过，2-拒绝
    private String auditRemark;
}