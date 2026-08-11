package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EvaluationVO {
    private Long id;
    private Long orderId;
    private Long roomId;
    private String roomTitle;
    private String roomCover;
    private Integer rating;
    private String content;
    private String images;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;

    // 评价人信息
    private Long userId;
    private String userNickname;
    private String userAvatar;
}