package com.example.demo.dto;

import lombok.Data;

@Data
public class AppointmentHandleDTO {
    private Long appointmentId;
    private Integer status;
    private String landlordRemark;
}