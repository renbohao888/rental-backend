package com.example.demo.dto;

import lombok.Data;

@Data
public class RoomStatsVO {
    private Integer totalRooms;
    private Integer publishedRooms;
    private Integer rentedRooms;
    private Integer pendingAuditRooms;
}