package com.example.demo.dto;

import lombok.Data;

@Data
public class RepairSubmitDTO {
    private Long roomId;
    private String title;
    private String description;
    /** 图片URL列表，逗号分隔字符串（JSON 提交时使用） */
    private String images;

}