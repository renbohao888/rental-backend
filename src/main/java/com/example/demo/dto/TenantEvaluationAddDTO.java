package com.example.demo.dto;

import lombok.Data;

/**
 * 租客端发表评价请求体（对齐前端 my/Evaluations.vue 的 JSON）
 */
@Data
public class TenantEvaluationAddDTO {
    private Long orderId;
    private Long roomId;
    private Integer rating;
    private String content;
    private String images;
}
