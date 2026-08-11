package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LandlordApplicationVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userPhone;
    private String realName;
    private String idCard;
    private String phone;
    private String idCardFront;
    private String idCardBack;
    private String businessLicense;
    private String remark;
    private Integer status;
    private String statusText;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
}