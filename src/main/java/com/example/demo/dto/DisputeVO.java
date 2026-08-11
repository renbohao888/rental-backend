package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DisputeVO {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long roomId;
    private String roomTitle;
    private String roomCover;
    private Long userId;
    private String userNickname;
    private String userPhone;
    private String reason;
    private String description;
    private String evidenceImages;
    private Integer status;
    private String statusText;
    private String adminRemark;
    private String resolution;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}