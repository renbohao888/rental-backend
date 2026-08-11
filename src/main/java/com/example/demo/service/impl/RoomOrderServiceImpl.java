package com.example.demo.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.*;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomOrder;
import com.example.demo.entity.User;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.mapper.RoomOrderMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.RoomOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomOrderServiceImpl extends ServiceImpl<RoomOrderMapper, RoomOrder> implements RoomOrderService {

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private UserMapper userMapper;  // 👈 新增注入

    /**
     * 核心下单逻辑：自带防超卖校验
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoomOrder createOrder(Long userId, Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {

        // 入参合法性校验
        if (checkInDate.isAfter(checkOutDate) || checkInDate.isEqual(checkOutDate)) {
            throw new RuntimeException("入住日期必须早于退租日期！");
        }

        // 1. 防超卖日期重叠校验
        LambdaQueryWrapper<RoomOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoomOrder::getRoomId, roomId)
                .eq(RoomOrder::getIsDeleted, 0)
                .in(RoomOrder::getStatus, 0, 1, 2, 3)
                .lt(RoomOrder::getCheckInDate, checkOutDate)
                .gt(RoomOrder::getCheckOutDate, checkInDate);

        Long conflictCount = baseMapper.selectCount(queryWrapper);
        if (conflictCount > 0) {
            throw new RuntimeException("该房源在此时间段内已被预订，请重新选择日期！");
        }

        // 2. 查询房源真实信息，生成快照
        Room room = roomMapper.selectById(roomId);
        if (room == null || room.getStatus() != 1) {
            throw new RuntimeException("房源不存在或已下架！");
        }

        // 3. 计算入住晚数与总金额
        long stayDays = checkOutDate.toEpochDay() - checkInDate.toEpochDay();
        BigDecimal totalAmount = room.getPrice().multiply(BigDecimal.valueOf(stayDays));

        // 4. 组装订单数据
        RoomOrder order = new RoomOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setRoomId(roomId);
        order.setRoomTitleSnapshot(room.getTitle());
        order.setRoomCoverSnapshot(room.getCover());
        order.setTotalAmount(totalAmount);
        order.setDeposit(room.getDeposit());
        order.setCheckInDate(checkInDate);
        order.setCheckOutDate(checkOutDate);
        order.setStatus(0);

        baseMapper.insert(order);
        return order;
    }

    // ================== 原有基础方法保留 ==================
    @Override
    public List<RoomOrder> getUserOrders(Long userId) {
        return baseMapper.selectList(new LambdaQueryWrapper<RoomOrder>().eq(RoomOrder::getUserId, userId));
    }

    @Override
    public IPage<RoomOrder> getUserOrdersPage(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<RoomOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RoomOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomOrder::getUserId, userId)
                .orderByDesc(RoomOrder::getCreateTime);
        if (status != null) {
            wrapper.eq(RoomOrder::getStatus, status);
        }
        return this.page(page, wrapper);
    }

    @Override
    public boolean deleteOrder(Long orderId, Long userId) {
        RoomOrder order = this.getById(orderId);
        if (order == null) {
            return false;
        }
        if (!order.getUserId().equals(userId)) {
            return false;
        }
        return this.removeById(orderId);
    }

    @Override
    public int batchDeleteOrders(List<Long> orderIds, Long userId) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<RoomOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RoomOrder::getId, orderIds)
                .eq(RoomOrder::getUserId, userId);
        List<RoomOrder> userOrders = baseMapper.selectList(wrapper);

        if (userOrders.isEmpty()) {
            return 0;
        }
        List<Long> deletableIds = userOrders.stream()
                .map(RoomOrder::getId)
                .toList();
        boolean success = this.removeByIds(deletableIds);
        return success ? deletableIds.size() : 0;
    }

    @Override
    public boolean deleteLandlordOrder(Long orderId, Long landlordId) {
        RoomOrder order = this.getById(orderId);
        if (order == null) {
            return false;
        }
        Room room = roomMapper.selectById(order.getRoomId());
        if (room == null || !room.getLandlordId().equals(landlordId)) {
            return false;
        }
        return this.removeById(orderId);
    }

    @Override
    public int batchDeleteLandlordOrders(List<Long> orderIds, Long landlordId) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }
        List<RoomOrder> all = this.listByIds(orderIds);
        List<Long> deletableIds = new ArrayList<>();
        for (RoomOrder order : all) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room != null && room.getLandlordId().equals(landlordId)) {
                deletableIds.add(order.getId());
            }
        }
        if (deletableIds.isEmpty()) {
            return 0;
        }
        boolean success = this.removeByIds(deletableIds);
        return success ? deletableIds.size() : 0;
    }

    @Override
    public boolean forceDeleteOrder(Long orderId) {
        // 使用自定义物理删除 SQL，绕过 @TableLogic 逻辑删除
        return baseMapper.physicallyDeleteById(orderId) > 0;
    }

    @Override
    public String generateOrderNo() {
        return "ORD" + IdUtil.getSnowflake(1, 1).nextIdStr();
    }

    // ================== 退租闭环方法 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkIn(Long orderId, Long currentUserId, Integer role) {
        RoomOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (role != null && role == 0) {
            // 管理员直接放行
        } else if (role != null && role == 1) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room == null || !room.getLandlordId().equals(currentUserId)) {
                throw new RuntimeException("无权操作他人房源的订单");
            }
        } else if (role != null && role == 2) {
            if (!order.getUserId().equals(currentUserId)) {
                throw new RuntimeException("无权操作他人订单");
            }
        } else {
            throw new RuntimeException("角色信息异常");
        }

        if (order.getStatus() != 1) {
            throw new RuntimeException("当前订单状态不可确认入住");
        }

        order.setStatus(2);
        return this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyCheckOut(Long orderId, Long userId) {
        RoomOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作他人订单");
        }
        if (order.getStatus() != 2) {
            throw new RuntimeException("当前订单状态不可申请退房");
        }
        order.setStatus(3);
        return this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeOrder(Long orderId, Long currentUserId, Integer role, BigDecimal deductAmount) {
        if (deductAmount == null || deductAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("扣款金额参数非法");
        }
        RoomOrder order = this.getById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");

        if (role != null && role != 0) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room == null || !room.getLandlordId().equals(currentUserId)) {
                throw new RuntimeException("无权操作他人房源的订单");
            }
        }

        if (order.getStatus() != 3) throw new RuntimeException("当前订单状态不可完结");

        BigDecimal refundAmount = order.getDeposit().subtract(deductAmount);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) refundAmount = BigDecimal.ZERO;

        order.setStatus(4);
        boolean updateResult = this.updateById(order);

        if (updateResult) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room != null && room.getStatus() == 2) {
                LambdaUpdateWrapper<Room> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Room::getId, order.getRoomId())
                        .set(Room::getStatus, 1);
                roomMapper.update(null, updateWrapper);
            }
        }
        return updateResult;
    }

    // ================== 撤销订单+房东拒单 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId) {
        RoomOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权撤销他人订单");
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new RuntimeException("当前订单状态不可撤销");
        }
        order.setStatus(5);
        boolean updateResult = this.updateById(order);

        if (updateResult) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room != null && room.getStatus() == 2) {
                LambdaUpdateWrapper<Room> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Room::getId, order.getRoomId())
                        .set(Room::getStatus, 1);
                roomMapper.update(null, updateWrapper);
            }
        }
        return updateResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectOrder(Long orderId, Long landlordId) {
        RoomOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        Room room = roomMapper.selectById(order.getRoomId());
        if (room == null || !room.getLandlordId().equals(landlordId)) {
            throw new RuntimeException("无权操作他人房源的订单");
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new RuntimeException("当前订单状态不可拒单");
        }
        order.setStatus(6);
        boolean updateResult = this.updateById(order);

        if (updateResult && room.getStatus() == 2) {
            LambdaUpdateWrapper<Room> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Room::getId, order.getRoomId())
                    .set(Room::getStatus, 1);
            roomMapper.update(null, updateWrapper);
        }
        return updateResult;
    }

    // ================== 查询接口 ==================

    @Override
    public IPage<RoomOrder> getAllOrdersPage(Integer pageNum, Integer pageSize, Integer status) {
        Page<RoomOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RoomOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(RoomOrder::getStatus, status);
        }
        wrapper.orderByDesc(RoomOrder::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public List<RoomOrder> getLandlordOrders(Long landlordId) {
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getLandlordId, landlordId)
                .select(Room::getId);
        List<Long> roomIds = roomMapper.selectList(roomWrapper)
                .stream()
                .map(Room::getId)
                .toList();

        if (roomIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<RoomOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(RoomOrder::getRoomId, roomIds)
                .orderByDesc(RoomOrder::getCreateTime);
        return baseMapper.selectList(orderWrapper);
    }

    @Override
    public List<RoomBillSummaryVO> getLandlordBillSummary(Long landlordId) {
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getLandlordId, landlordId)
                .select(Room::getId, Room::getTitle);
        List<Room> rooms = roomMapper.selectList(roomWrapper);

        if (rooms.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .toList();

        LambdaQueryWrapper<RoomOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(RoomOrder::getRoomId, roomIds)
                .notIn(RoomOrder::getStatus, 5, 6);
        List<RoomOrder> orders = baseMapper.selectList(orderWrapper);

        Map<Long, List<RoomOrder>> groupMap = orders.stream()
                .collect(Collectors.groupingBy(RoomOrder::getRoomId));

        List<RoomBillSummaryVO> result = new ArrayList<>();
        for (Room room : rooms) {
            List<RoomOrder> orderList = groupMap.getOrDefault(room.getId(), new ArrayList<>());
            int totalOrders = orderList.size();

            List<RoomOrder> completedList = orderList.stream()
                    .filter(o -> o.getStatus() == 4)
                    .toList();

            int completedOrders = completedList.size();

            BigDecimal totalRevenue = completedList.stream()
                    .map(RoomOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDeposit = completedList.stream()
                    .map(RoomOrder::getDeposit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            RoomBillSummaryVO vo = new RoomBillSummaryVO();
            vo.setRoomId(room.getId());
            vo.setRoomTitle(room.getTitle());
            vo.setTotalOrders(totalOrders);
            vo.setCompletedOrders(completedOrders);
            vo.setTotalRevenue(totalRevenue);
            vo.setTotalDeposit(totalDeposit);

            result.add(vo);
        }

        result.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        return result;
    }

    // ================== 统计数据接口 ==================

    @Override
    public BusinessStatsVO getBusinessStats(Long landlordId) {
        BusinessStatsVO stats = new BusinessStatsVO();

        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getLandlordId, landlordId);
        List<Room> rooms = roomMapper.selectList(roomWrapper);

        stats.setTotalRooms(rooms.size());
        stats.setPublishedRooms((int) rooms.stream().filter(r -> r.getStatus() == 1).count());
        stats.setRentedRooms((int) rooms.stream().filter(r -> r.getStatus() == 2).count());
        stats.setPendingAuditRooms((int) rooms.stream().filter(r -> r.getStatus() == 0).count());

        if (rooms.isEmpty()) {
            stats.setTotalOrders(0);
            stats.setThisMonthOrders(0);
            stats.setPendingOrders(0);
            stats.setTotalRevenue(BigDecimal.ZERO);
            stats.setThisMonthRevenue(BigDecimal.ZERO);
            stats.setAvgOrderAmount(BigDecimal.ZERO);
            stats.setDailyTrend(new ArrayList<>());
            return stats;
        }

        List<Long> roomIds = rooms.stream().map(Room::getId).toList();

        LambdaQueryWrapper<RoomOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(RoomOrder::getRoomId, roomIds)
                .notIn(RoomOrder::getStatus, 5, 6);
        List<RoomOrder> orders = baseMapper.selectList(orderWrapper);

        stats.setTotalOrders(orders.size());

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        long thisMonthCount = orders.stream()
                .filter(o -> o.getCreateTime() != null &&
                        o.getCreateTime().isAfter(monthStart) &&
                        o.getCreateTime().isBefore(monthEnd))
                .count();
        stats.setThisMonthOrders((int) thisMonthCount);

        long pendingCount = orders.stream()
                .filter(o -> o.getStatus() == 0 || o.getStatus() == 1)
                .count();
        stats.setPendingOrders((int) pendingCount);

        List<RoomOrder> completedOrders = orders.stream()
                .filter(o -> o.getStatus() == 4)
                .toList();

        BigDecimal totalRevenue = completedOrders.stream()
                .map(RoomOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        BigDecimal thisMonthRevenue = completedOrders.stream()
                .filter(o -> o.getCreateTime() != null &&
                        o.getCreateTime().isAfter(monthStart) &&
                        o.getCreateTime().isBefore(monthEnd))
                .map(RoomOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setThisMonthRevenue(thisMonthRevenue);

        if (!completedOrders.isEmpty()) {
            stats.setAvgOrderAmount(totalRevenue.divide(BigDecimal.valueOf(completedOrders.size()), 2, RoundingMode.HALF_UP));
        } else {
            stats.setAvgOrderAmount(BigDecimal.ZERO);
        }

        stats.setDailyTrend(getDailyTrend(roomIds, 7));
        return stats;
    }

    private List<DailyTrendVO> getDailyTrend(List<Long> roomIds, int days) {
        List<DailyTrendVO> trendList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        List<LocalDate> dateList = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            dateList.add(today.minusDays(i));
        }

        LocalDateTime startTime = dateList.get(0).atStartOfDay();
        LocalDateTime endTime = dateList.get(dateList.size() - 1).atTime(23, 59, 59);

        LambdaQueryWrapper<RoomOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RoomOrder::getRoomId, roomIds)
                .eq(RoomOrder::getStatus, 4)
                .ge(RoomOrder::getCreateTime, startTime)
                .le(RoomOrder::getCreateTime, endTime);
        List<RoomOrder> orders = baseMapper.selectList(wrapper);

        Map<LocalDate, List<RoomOrder>> groupMap = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreateTime().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (LocalDate date : dateList) {
            DailyTrendVO vo = new DailyTrendVO();
            vo.setDate(date);
            List<RoomOrder> dayOrders = groupMap.getOrDefault(date, new ArrayList<>());
            vo.setOrderCount(dayOrders.size());
            BigDecimal dayRevenue = dayOrders.stream()
                    .map(RoomOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setRevenue(dayRevenue);
            trendList.add(vo);
        }

        return trendList;
    }

    @Override
    public DashboardVO getDashboardStats() {
        DashboardVO dashboard = new DashboardVO();

        UserStatsVO userStats = getUserStats();
        dashboard.setUserStats(userStats);

        RoomStatsVO roomStats = getRoomStats();
        dashboard.setRoomStats(roomStats);

        List<RoomOrder> allOrders = baseMapper.selectList(
                new LambdaQueryWrapper<RoomOrder>()
                        .notIn(RoomOrder::getStatus, 5, 6)
        );

        OrderStatsVO orderStats = getOrderStats(allOrders);
        dashboard.setOrderStats(orderStats);

        RevenueStatsVO revenueStats = getRevenueStats(allOrders);
        dashboard.setRevenueStats(revenueStats);

        List<DailyTrendVO> recentTrend = getRecentTrend(allOrders, 7);
        dashboard.setRecentTrend(recentTrend);

        List<HotRoomVO> hotRooms = getHotRooms(allOrders, 5);
        dashboard.setHotRooms(hotRooms);

        return dashboard;
    }

    private UserStatsVO getUserStats() {
        UserStatsVO stats = new UserStatsVO();
        List<User> users = userMapper.selectList(null);
        stats.setTotalUsers(users.size());
        stats.setTenantCount((int) users.stream().filter(u -> u.getRole() == 2).count());
        stats.setLandlordCount((int) users.stream().filter(u -> u.getRole() == 1).count());
        stats.setAdminCount((int) users.stream().filter(u -> u.getRole() == 0).count());
        return stats;
    }

    private RoomStatsVO getRoomStats() {
        RoomStatsVO stats = new RoomStatsVO();
        List<Room> rooms = roomMapper.selectList(null);
        stats.setTotalRooms(rooms.size());
        stats.setPublishedRooms((int) rooms.stream().filter(r -> r.getStatus() == 1).count());
        stats.setRentedRooms((int) rooms.stream().filter(r -> r.getStatus() == 2).count());
        stats.setPendingAuditRooms((int) rooms.stream().filter(r -> r.getStatus() == 0).count());
        return stats;
    }

    private OrderStatsVO getOrderStats(List<RoomOrder> orders) {
        OrderStatsVO stats = new OrderStatsVO();
        stats.setTotalOrders(orders.size());

        long completed = orders.stream().filter(o -> o.getStatus() == 4).count();
        stats.setCompletedOrders((int) completed);

        long pending = orders.stream().filter(o -> o.getStatus() == 0 || o.getStatus() == 1).count();
        stats.setPendingOrders((int) pending);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        long thisMonth = orders.stream()
                .filter(o -> o.getCreateTime() != null &&
                        o.getCreateTime().isAfter(monthStart) &&
                        o.getCreateTime().isBefore(monthEnd))
                .count();
        stats.setThisMonthOrders((int) thisMonth);

        return stats;
    }

    private RevenueStatsVO getRevenueStats(List<RoomOrder> orders) {
        RevenueStatsVO stats = new RevenueStatsVO();

        List<RoomOrder> completedOrders = orders.stream()
                .filter(o -> o.getStatus() == 4)
                .toList();

        BigDecimal totalRevenue = completedOrders.stream()
                .map(RoomOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        BigDecimal thisMonthRevenue = completedOrders.stream()
                .filter(o -> o.getCreateTime() != null &&
                        o.getCreateTime().isAfter(monthStart) &&
                        o.getCreateTime().isBefore(monthEnd))
                .map(RoomOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setThisMonthRevenue(thisMonthRevenue);

        if (!completedOrders.isEmpty()) {
            stats.setAvgOrderAmount(totalRevenue.divide(
                    BigDecimal.valueOf(completedOrders.size()), 2, RoundingMode.HALF_UP
            ));
        } else {
            stats.setAvgOrderAmount(BigDecimal.ZERO);
        }

        return stats;
    }

    private List<DailyTrendVO> getRecentTrend(List<RoomOrder> allOrders, int days) {
        List<DailyTrendVO> trendList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        List<LocalDate> dateList = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            dateList.add(today.minusDays(i));
        }

        List<RoomOrder> completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == 4)
                .toList();

        Map<LocalDate, List<RoomOrder>> groupMap = completedOrders.stream()
                .filter(o -> o.getCreateTime() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getCreateTime().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (LocalDate date : dateList) {
            DailyTrendVO vo = new DailyTrendVO();
            vo.setDate(date);
            List<RoomOrder> dayOrders = groupMap.getOrDefault(date, new ArrayList<>());
            vo.setOrderCount(dayOrders.size());
            BigDecimal dayRevenue = dayOrders.stream()
                    .map(RoomOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setRevenue(dayRevenue);
            trendList.add(vo);
        }

        return trendList;
    }

    private List<HotRoomVO> getHotRooms(List<RoomOrder> orders, int topN) {
        List<RoomOrder> completedOrders = orders.stream()
                .filter(o -> o.getStatus() == 4)
                .toList();

        Map<Long, List<RoomOrder>> groupMap = completedOrders.stream()
                .filter(o -> o.getRoomId() != null)
                .collect(Collectors.groupingBy(RoomOrder::getRoomId));

        List<HotRoomVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<RoomOrder>> entry : groupMap.entrySet()) {
            Long roomId = entry.getKey();
            List<RoomOrder> roomOrders = entry.getValue();

            HotRoomVO vo = new HotRoomVO();
            vo.setRoomId(roomId);
            vo.setOrderCount(roomOrders.size());

            BigDecimal totalRevenue = roomOrders.stream()
                    .map(RoomOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setTotalRevenue(totalRevenue);

            Room room = roomMapper.selectById(roomId);
            if (room != null) {
                vo.setRoomTitle(room.getTitle());
                vo.setRoomCover(room.getCover());
            }

            result.add(vo);
        }

        result.sort((a, b) -> b.getOrderCount().compareTo(a.getOrderCount()));
        return result.size() > topN ? result.subList(0, topN) : result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminForceRefund(Long orderId, Long adminId, String reason) {
        RoomOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 允许退款的状态：0-待支付，1-已支付待入住，2-已入住，3-退租中
        if (order.getStatus() != 0 && order.getStatus() != 1 &&
                order.getStatus() != 2 && order.getStatus() != 3) {
            throw new RuntimeException("当前订单状态不可强制退款");
        }

        // 更新订单状态为已取消(5)，记录退款原因
        order.setStatus(5);
        order.setAdminRemark("强制退款，操作人：" + adminId + "，原因：" + reason);
        boolean updateResult = this.updateById(order);

        // 如果房源被锁定，释放房源
        if (updateResult) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room != null && room.getStatus() == 2) {
                LambdaUpdateWrapper<Room> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(Room::getId, order.getRoomId())
                        .set(Room::getStatus, 1);
                roomMapper.update(null, wrapper);
            }
        }
        return updateResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminForceCheckout(Long orderId, Long adminId, String reason) {
        RoomOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 允许强制退房的状态：2-已入住，3-退租中
        if (order.getStatus() != 2 && order.getStatus() != 3) {
            throw new RuntimeException("当前订单状态不可强制退房");
        }

        // 直接完结订单（不扣押金）
        order.setStatus(4);
        order.setAdminRemark("强制退房，操作人：" + adminId + "，原因：" + reason);
        boolean updateResult = this.updateById(order);

        // 释放房源
        if (updateResult) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room != null && room.getStatus() == 2) {
                LambdaUpdateWrapper<Room> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(Room::getId, order.getRoomId())
                        .set(Room::getStatus, 1);
                roomMapper.update(null, wrapper);
            }
        }
        return updateResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminForceRemoveRoom(Long roomId, Long adminId, String reason) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new RuntimeException("房源不存在");
        }

        // 1. 将该房源所有待处理订单（status=0,1,2,3）取消
        LambdaQueryWrapper<RoomOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(RoomOrder::getRoomId, roomId)
                .in(RoomOrder::getStatus, 0, 1, 2, 3);
        List<RoomOrder> orders = baseMapper.selectList(orderWrapper);

        for (RoomOrder order : orders) {
            order.setStatus(5);
            order.setAdminRemark("房源被管理员强制撤销，操作人：" + adminId + "，原因：" + reason);
            this.updateById(order);
        }

        // 2. 下架房源（status=3 已下架）
        room.setStatus(3);
        room.setAdminRemark("管理员强制下架，原因：" + reason);
        roomMapper.updateById(room);

        return true;
    }
}