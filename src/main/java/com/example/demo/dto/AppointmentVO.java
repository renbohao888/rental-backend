package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppointmentVO {
    private Long id;
    private Long roomId;
    private String roomTitle;
    private String roomCover;
    private String roomAddress;
    private Long userId;
    private String userNickname;
    private String userPhone;
    private LocalDate appointmentDate;
    private String appointmentTime;
    private String remark;
    private Integer status;
    private String statusText;
    private String landlordRemark;
    private LocalDateTime confirmTime;
    private LocalDateTime viewTime;
    private LocalDateTime createTime;
}