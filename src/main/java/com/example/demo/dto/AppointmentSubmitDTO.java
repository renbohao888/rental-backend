package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AppointmentSubmitDTO {
    private Long roomId;
    private LocalDate appointmentDate;
    private String appointmentTime;
    private String remark;
}