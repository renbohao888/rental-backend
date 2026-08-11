package com.example.demo.dto;

import lombok.Data;

/**
 * 租客端提交纠纷的请求体（对齐前端 my/Disputes.vue 的 JSON）
 */
@Data
public class TenantDisputeSubmitDTO {
    private Long orderId;
    /** 纠纷类型：landlord_breach-房东违约 room_issue-房屋问题 other-其他 */
    private String type;
    private String description;
    /** 索赔金额（可选） */
    private java.math.BigDecimal amount;
}
