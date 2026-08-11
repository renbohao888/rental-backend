package com.example.demo.dto;

import lombok.Data;

@Data
public class EvaluationSubmitDTO {
    private Long orderId;
    private Integer rating; // 1-5
    private String content;
}