package com.example.demo.dto;

import lombok.Data;

@Data
public class DisputeSubmitDTO {
    private Long orderId;
    private String reason;
    private String description;
}