package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("room_order")
public class RoomOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;

    private String alipayTradeNo;
    private String orderNo;
    private Long userId;
    private Long roomId;

    private String roomTitleSnapshot;
    private String roomCoverSnapshot;
    private BigDecimal totalAmount;
    // 订单押金快照（下单时同步房源押金存入）
    private BigDecimal deposit;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    /**
     * 订单状态字典
     * 0-待支付
     * 1-已支付待入住
     * 2-已入住
     * 3-退租核算中
     * 4-已完成
     * 5-已取消（租客主动撤销）
     * 6-已拒绝（房东拒单）
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /**
     * 管理员备注（用于强制退款/强制退房等操作记录）
     */
    private String adminRemark;
}