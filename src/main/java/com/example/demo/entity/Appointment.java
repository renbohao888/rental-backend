package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long roomId;

    private LocalDate appointmentDate;

    private String appointmentTime;

    private String remark;

    private Integer status; // 0-待确认，1-已确认，2-已拒绝，3-已看房

    private String landlordRemark;

    private LocalDateTime confirmTime;

    private LocalDateTime viewTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}