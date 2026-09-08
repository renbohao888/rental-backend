package com.anju.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.anju.rental.entity.Room;
import com.anju.rental.entity.RoomOrder;
import com.anju.rental.mapper.RoomMapper;
import com.anju.rental.mapper.RoomOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTimeoutScheduler {

    @Autowired
    private RoomOrderMapper roomOrderMapper;

    @Autowired
    private RoomMapper roomMapper;

    // 从配置文件读取超时分钟数，默认3分钟
    @Value("${order.pay.timeout:3}")
    private long timeoutMinutes;

    /**
     * 定时任务：每 60 秒执行一次，扫描超时未支付的订单并自动取消
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrders() {
        // 计算超时时间点（当前时间减去超时分钟数）
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // 查询超时未支付的订单（status=0 且 create_time < timeoutTime）
        LambdaQueryWrapper<RoomOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoomOrder::getStatus, 0)
                .lt(RoomOrder::getCreateTime, timeoutTime);

        List<RoomOrder> timeoutOrders = roomOrderMapper.selectList(queryWrapper);

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("定时任务：发现 {} 笔超时未支付订单，开始自动取消", timeoutOrders.size());

        for (RoomOrder order : timeoutOrders) {
            try {
                // 更新订单状态为 5（已取消）
                order.setStatus(5);
                roomOrderMapper.updateById(order);

                // 释放对应的房源（使用 LambdaUpdateWrapper 绕过乐观锁）
                Room room = roomMapper.selectById(order.getRoomId());
                if (room != null && room.getStatus() == 2) {
                    LambdaUpdateWrapper<Room> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(Room::getId, order.getRoomId())
                            .set(Room::getStatus, 1);
                    roomMapper.update(null, updateWrapper);
                    log.info("订单 {} 已自动取消，房源 {} 已释放", order.getOrderNo(), order.getRoomId());
                }
            } catch (Exception e) {
                log.error("处理超时订单 {} 失败", order.getOrderNo(), e);
            }
        }
    }
}