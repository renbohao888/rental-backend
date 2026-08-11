package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomOrder;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.mapper.RoomOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
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
        // 1. 计算超时时间点（当前时间减去超时分钟数）
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // 2. 查询超时未支付的订单（status=0 且 create_time < timeoutTime）
        LambdaQueryWrapper<RoomOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoomOrder::getStatus, 0)
                .lt(RoomOrder::getCreateTime, timeoutTime);

        List<RoomOrder> timeoutOrders = roomOrderMapper.selectList(queryWrapper);

        if (timeoutOrders.isEmpty()) {
            return;
        }

        System.out.println("🔔 定时任务：发现 " + timeoutOrders.size() + " 笔超时未支付订单，开始自动取消...");

        for (RoomOrder order : timeoutOrders) {
            try {
                // 3. 更新订单状态为 5（已取消）
                order.setStatus(5);
                roomOrderMapper.updateById(order);

                // 4. 释放对应的房源（使用 LambdaUpdateWrapper 绕过乐观锁）
                Room room = roomMapper.selectById(order.getRoomId());
                if (room != null && room.getStatus() == 2) {
                    LambdaUpdateWrapper<Room> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(Room::getId, order.getRoomId())
                            .set(Room::getStatus, 1);
                    roomMapper.update(null, updateWrapper);
                    System.out.println("   ✅ 订单 " + order.getOrderNo() + " 已自动取消，房源 " + order.getRoomId() + " 已释放");
                }
            } catch (Exception e) {
                System.err.println("   ❌ 处理超时订单 " + order.getOrderNo() + " 失败：" + e.getMessage());
            }
        }
    }
}