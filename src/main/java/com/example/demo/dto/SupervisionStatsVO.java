package com.example.demo.dto;

import lombok.Data;

@Data
public class SupervisionStatsVO {
    // 报修统计
    private Integer totalRepairs;
    private Integer pendingRepairs;   // 待处理
    private Integer processingRepairs; // 处理中
    private Integer completedRepairs; // 已完成

    // 纠纷统计
    private Integer totalDisputes;
    private Integer pendingDisputes;  // 待受理
    private Integer processingDisputes; // 处理中
    private Integer resolvedDisputes; // 已解决
    private Integer rejectedDisputes; // 已驳回
}