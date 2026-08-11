package com.example.demo.dto;

import lombok.Data;

@Data
public class UserStatsVO {
    private Integer totalUsers;
    private Integer tenantCount;
    private Integer landlordCount;
    private Integer adminCount;
}