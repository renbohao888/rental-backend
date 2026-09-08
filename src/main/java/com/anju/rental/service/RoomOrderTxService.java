package com.anju.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.anju.rental.entity.Room;
import com.anju.rental.entity.RoomOrder;
import com.anju.rental.mapper.RoomMapper;
import com.anju.rental.mapper.RoomOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 下单“写库”事务执行器。
 *
 * <p>把下单的数据库操作收敛到独立的 Spring Bean 中，并通过 @Transactional 保证：
 * “时间重叠校验 + 订单落库 + 事务提交”完整发生在外层分布式锁的临界区内，
 * 锁释放一定晚于事务提交，从根上避免并发下单产生同一房源同一时间段的重叠订单（防超卖）。</p>
 */
@Service
public class RoomOrderTxService {

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private RoomOrderMapper roomOrderMapper;

    @Transactional(rollbackFor = Exception.class)
    public RoomOrder createOrderInTx(Long userId, Long roomId, String orderNo,
                                     LocalDate checkInDate, LocalDate checkOutDate) {

        // 防超卖核心校验：查询同一房源上“时间段存在重叠”的有效订单
        // 状态 0-待支付、1-已支付待入住、2-已入住、3-退租核算中 都视为占用时段
        LambdaQueryWrapper<RoomOrder> conflictWrapper = new LambdaQueryWrapper<>();
        conflictWrapper.eq(RoomOrder::getRoomId, roomId)
                .eq(RoomOrder::getIsDeleted, 0)
                .in(RoomOrder::getStatus, 0, 1, 2, 3)
                .lt(RoomOrder::getCheckInDate, checkOutDate)
                .gt(RoomOrder::getCheckOutDate, checkInDate);

        if (roomOrderMapper.selectCount(conflictWrapper) > 0) {
            throw new RuntimeException("该房源在此时间段内已被预订，请重新选择日期！");
        }

        // 查询房源真实信息，生成快照
        Room room = roomMapper.selectById(roomId);
        if (room == null || room.getStatus() != 1) {
            throw new RuntimeException("房源不存在或已下架！");
        }

        // 计算入住晚数与总金额
        long stayDays = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        BigDecimal totalAmount = room.getPrice().multiply(BigDecimal.valueOf(stayDays));

        // 组装订单数据
        RoomOrder order = new RoomOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setRoomId(roomId);
        order.setRoomTitleSnapshot(room.getTitle());
        order.setRoomCoverSnapshot(room.getCover());
        order.setTotalAmount(totalAmount);
        order.setDeposit(room.getDeposit());
        order.setCheckInDate(checkInDate);
        order.setCheckOutDate(checkOutDate);
        order.setStatus(0);

        roomOrderMapper.insert(order);
        return order;
    }
}
