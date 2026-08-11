package com.example.demo.dto;

import java.time.LocalDate;

public class OrderCreateDTO {
    private Long roomId;
    private LocalDate checkInDate;  // 入住日期
    private LocalDate checkOutDate; // 退租日期

    // Getter 和 Setter
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
}