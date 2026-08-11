package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.DashboardVO;
import com.example.demo.dto.LandlordDashboardVO;
import com.example.demo.dto.RecentOrderVO;
import com.example.demo.entity.Repair;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomOrder;
import com.example.demo.entity.User;
import com.example.demo.service.RepairService;
import com.example.demo.service.RoomOrderService;
import com.example.demo.service.RoomService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private RoomOrderService roomOrderService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RepairService repairService;

    @Autowired
    private UserService userService;

    /**
     * 平台运营数据大屏（仅管理员可调用）
     */
    @GetMapping("/admin/stats")
    @RequiresRoles({0})
    public Result<DashboardVO> getDashboardStats() {
        DashboardVO stats = roomOrderService.getDashboardStats();
        return Result.success(stats);
    }

    /**
     * 房东经营数据仪表盘（仅房东可调用）
     * 返回该房东名下的房源/订单/收入/评分/报修统计
     */
    @GetMapping("/landlord/stats")
    @RequiresRoles({1})
    public Result<LandlordDashboardVO> getLandlordStats(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        LandlordDashboardVO vo = new LandlordDashboardVO();

        // ===== 1. 房源统计 =====
        List<Room> rooms = roomService.getMyRooms(landlordId);
        vo.setTotalRooms(rooms.size());
        vo.setPublishedRooms((int) rooms.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count());
        vo.setRentedRooms((int) rooms.stream().filter(r -> r.getStatus() != null && r.getStatus() == 2).count());
        vo.setPendingAuditRooms((int) rooms.stream().filter(r -> r.getStatus() != null && r.getStatus() == 0).count());
        vo.setRejectedRooms((int) rooms.stream().filter(r -> r.getStatus() != null && r.getStatus() == 4).count());

        // ===== 2. 平均评分（房源评分均值） =====
        OptionalDouble avg = rooms.stream()
                .filter(r -> r.getRating() != null)
                .mapToDouble(r -> r.getRating().doubleValue())
                .average();
        vo.setAvgRating(avg.isPresent()
                ? BigDecimal.valueOf(avg.getAsDouble()).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // ===== 3. 订单统计 =====
        List<RoomOrder> orders = roomOrderService.getLandlordOrders(landlordId);
        vo.setTotalOrders(orders.size());
        // 进行中：1-已支付待入住、2-已入住、3-退租核算中
        vo.setActiveOrders((int) orders.stream()
                .filter(o -> o.getStatus() != null && (o.getStatus() == 1 || o.getStatus() == 2 || o.getStatus() == 3))
                .count());

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<RoomOrder> validOrders = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() != 5 && o.getStatus() != 6)
                .toList();
        vo.setThisMonthOrders((int) validOrders.stream()
                .filter(o -> o.getCreateTime() != null
                        && o.getCreateTime().isAfter(monthStart)
                        && o.getCreateTime().isBefore(monthEnd))
                .count());

        List<RoomOrder> completedOrders = validOrders.stream()
                .filter(o -> o.getStatus() == 4)
                .toList();
        BigDecimal totalRevenue = completedOrders.stream()
                .map(RoomOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalRevenue(totalRevenue);

        BigDecimal thisMonthRevenue = completedOrders.stream()
                .filter(o -> o.getCreateTime() != null
                        && o.getCreateTime().isAfter(monthStart)
                        && o.getCreateTime().isBefore(monthEnd))
                .map(RoomOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setThisMonthRevenue(thisMonthRevenue);

        if (!completedOrders.isEmpty()) {
            vo.setAvgOrderAmount(totalRevenue.divide(BigDecimal.valueOf(completedOrders.size()), 2, RoundingMode.HALF_UP));
        } else {
            vo.setAvgOrderAmount(BigDecimal.ZERO);
        }

        // ===== 4. 最近订单 =====
        buildRecentOrders(vo, rooms, validOrders);

        // ===== 5. 报修统计 =====
        buildRepairStats(vo, landlordId);

        return Result.success(vo);
    }

    /**
     * 组装最近订单（取前5条，展示租客昵称）
     */
    private void buildRecentOrders(LandlordDashboardVO vo, List<Room> rooms, List<RoomOrder> validOrders) {
        Map<Long, String> roomTitleCache = rooms.stream()
                .collect(Collectors.toMap(Room::getId, Room::getTitle, (a, b) -> a));
        Map<Long, String> userCache = new HashMap<>();
        List<RecentOrderVO> recentOrders = new ArrayList<>();
        for (RoomOrder o : validOrders.stream().limit(5).toList()) {
            RecentOrderVO item = new RecentOrderVO();
            item.setId(o.getId());
            item.setOrderNo(o.getOrderNo());
            item.setRoomId(o.getRoomId());
            item.setRoomTitle(roomTitleCache.getOrDefault(o.getRoomId(), o.getRoomTitleSnapshot()));
            item.setRoomTitleSnapshot(o.getRoomTitleSnapshot());
            item.setRoomCoverSnapshot(o.getRoomCoverSnapshot());
            item.setTenantId(o.getUserId());
            item.setTenantName(userCache.computeIfAbsent(o.getUserId(), uid -> {
                User u = userService.getById(uid);
                return u == null ? ("用户" + uid) : u.getNickname();
            }));
            item.setTotalAmount(o.getTotalAmount());
            item.setDeposit(o.getDeposit());
            item.setCheckInDate(o.getCheckInDate());
            item.setCheckOutDate(o.getCheckOutDate());
            item.setStatus(o.getStatus());
            item.setStatusText(getOrderStatusText(o.getStatus()));
            item.setAlipayTradeNo(o.getAlipayTradeNo());
            item.setAdminRemark(o.getAdminRemark());
            item.setCreateTime(o.getCreateTime());
            recentOrders.add(item);
        }
        vo.setRecentOrders(recentOrders);
    }

    /**
     * 组装报修统计，供房东仪表盘提醒及时处理
     */
    private void buildRepairStats(LandlordDashboardVO vo, Long landlordId) {
        IPage<Repair> repairPage = repairService.getLandlordRepairs(landlordId, 1, 100, null);
        List<Repair> repairs = repairPage.getRecords();
        vo.setTotalRepairs(repairs.size());
        vo.setPendingRepairs((int) repairs.stream().filter(r -> r.getStatus() != null && r.getStatus() == 0).count());
        vo.setProcessingRepairs((int) repairs.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count());
        vo.setPendingRepairList(repairs.stream()
                .filter(r -> r.getStatus() != null && (r.getStatus() == 0 || r.getStatus() == 1))
                .sorted(Comparator.comparing(Repair::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList());
    }

    private String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付待入住";
            case 2: return "已入住";
            case 3: return "退租核算中";
            case 4: return "已完成";
            case 5: return "已取消";
            case 6: return "已拒绝";
            default: return "未知";
        }
    }
}
