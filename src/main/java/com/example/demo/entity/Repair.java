package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Repair implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID) // 使用雪花算法
    private Long id;

    private Long userId;

    private Long roomId;

    private String title;

    private String description;

    private String images; // JSON数组字符串，如 ["/uploads/repair/xxx.jpg"]

    private Integer status; // 0-待处理，1-处理中，2-已完成，3-已关闭

    private Long handlerId;

    private String handlerRemark;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}