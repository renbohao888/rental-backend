package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.RoomBillSummaryVO;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomOrder;
import com.example.demo.service.RoomOrderService;
import com.example.demo.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/room")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomOrderService roomOrderService;  // 新增注入

    /**
     * 房源多条件分页查询接口（公开浏览）
     * 权限控制：游客、租客仅能查询已上架（status=1）的房源；管理员、房东可查询全部状态
     */
    @GetMapping("/list")
    public Result<IPage<Room>> getRoomList(   // 返回类型改为 Result<IPage<Room>>
                                              HttpServletRequest request,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String title,
                                              @RequestParam(required = false) String address,
                                              @RequestParam(required = false) Integer minPrice,
                                              @RequestParam(required = false) Integer maxPrice,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String tag
    ) {
        Integer userRole = (Integer) request.getAttribute("userRole");
        if (userRole == null || userRole == 2) {
            status = 1;
        }
        IPage<Room> page = roomService.searchRoomPage(pageNum, pageSize, title, address, minPrice, maxPrice, status, tag);
        return Result.success(page);   // 用 Result 包一层
    }

    /**
     * 房源详情接口（公开，游客也能查看）
     * @param id 房源ID
     */
    @GetMapping("/detail/{id}")
    public Result<Room> getRoomDetail(@PathVariable Long id) {
        Room room = roomService.getById(id);
        // 已删除（status=5）的房源视为不存在
        if (room == null || (room.getStatus() != null && room.getStatus() == 5)) {
            return Result.fail("房源不存在");
        }
        return Result.success(room);
    }

    /**
     * 房源预定接口（仅租客可调用）
     */
    @PostMapping("/book")
    @RequiresRoles({2})
    public String bookRoom(HttpServletRequest request, @RequestBody Map<String, Long> params) {
        Long roomId = params.get("roomId");
        Long userId = (Long) request.getAttribute("realUserId");

        boolean success = roomService.bookRoom(roomId, userId);
        return success ? "预定成功！" : "手慢了，该房源已被抢走或正在处理中！";
    }

    /**
     * 发布房源（管理员、房东可调用）
     * 强制初始状态为0-待审核
     */
    @PostMapping("/add")
    @RequiresRoles({0, 1})
    public String addRoom(HttpServletRequest request, @RequestBody Room room) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        // 强制新发布房源为待审核状态
        room.setStatus(0);
        room.setLandlordId(landlordId);

        boolean success = roomService.save(room);
        return success ? "房源发布成功，等待管理员审核！" : "发布失败，请检查数据";
    }

    /**
     * 房源审核接口（仅管理员可调用）
     * @param roomId 房源ID
     * @param status 审核结果：1-审核通过上架、3-强制下架、4-审核驳回
     * @param remark 驳回理由（status=4 时必填，保存到 adminRemark）
     */
    @PostMapping("/audit/{roomId}")
    @RequiresRoles({0})
    public String auditRoom(@PathVariable Long roomId,
                            @RequestParam Integer status,
                            @RequestParam(required = false) String remark) {
        if (status != 1 && status != 3 && status != 4) {
            return "审核状态参数错误，仅支持1-审核通过、3-强制下架、4-审核驳回";
        }
        // 驳回时必须填写理由
        if (status == 4 && (remark == null || remark.trim().isEmpty())) {
            return "请填写驳回理由";
        }
        Room room = roomService.getById(roomId);
        if (room == null) {
            return "房源不存在，审核失败";
        }
        // 使用 UpdateWrapper 更新状态（不再依赖乐观锁，避免 MP 乐观锁绑定异常）
        LambdaUpdateWrapper<Room> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Room::getId, roomId)
                .set(Room::getStatus, status)
                // 驳回时写入理由，审核通过/强制下架时清空旧理由
                .set(Room::getAdminRemark, status == 4 ? remark.trim() : null)
                .set(Room::getUpdateTime, LocalDateTime.now());
        boolean success = roomService.update(new Room(), wrapper);
        return success ? "房源审核操作成功" : "房源审核操作失败，可能数据已被修改";
    }

    /**
     * 管理员分页查询全部房源（可按状态筛选，包含待审核）
     * 注意：不能复用 /room/list（该接口对游客开放且强制过滤已上架）
     */
    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public Result<IPage<Room>> getAdminRoomList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long landlordId) {
        IPage<Room> page;
        if (landlordId != null) {
            // 按房东过滤：管理员查看某房东名下的房源
            page = roomService.lambdaQuery()
                    .eq(Room::getLandlordId, landlordId)
                    .like(title != null && !title.isBlank(), Room::getTitle, title)
                    .eq(status != null, Room::getStatus, status)
                    .orderByDesc(Room::getCreateTime)
                    .page(new Page<>(pageNum, pageSize));
        } else {
            page = roomService.searchRoomPage(pageNum, pageSize, title, null, null, null, status, null);
        }
        return Result.success(page);
    }

    // ================== 新增：房东房源管理接口 ==================

    /**
     * 房东查看自己发布的所有房源
     */
    @GetMapping("/my")
    @RequiresRoles({1})
    public List<Room> getMyRooms(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        return roomService.getMyRooms(landlordId);
    }

    /**
     * 房东编辑房源信息
     */
    @PutMapping("/update")
    @RequiresRoles({1})
    public String updateRoom(HttpServletRequest request, @RequestBody Room room) {
        Long landlordId = (Long) request.getAttribute("realUserId");
        boolean success = roomService.updateRoom(landlordId, room);
        return success ? "房源更新成功" : "房源更新失败";
    }

    /**
     * 房东上架/下架房源
     * @param roomId 房源ID
     * @param status 状态：1-上架，3-下架，0-驳回后修改重新提交审核
     */
    @PostMapping("/status/{roomId}")
    @RequiresRoles({1})
    public String updateRoomStatus(HttpServletRequest request,
                                   @PathVariable Long roomId,
                                   @RequestParam Integer status) {
        if (status != 0 && status != 1 && status != 3) {
            return "状态参数错误，仅支持0-重新提交审核、1-上架、3-下架";
        }
        Long landlordId = (Long) request.getAttribute("realUserId");
        // 校验房源存在且属于该房东
        Room existRoom = roomService.getById(roomId);
        if (existRoom == null || !existRoom.getLandlordId().equals(landlordId)) {
            return "房源不存在或无权操作";
        }
        // 使用 UpdateWrapper 更新状态；重新提交审核时清空旧的驳回理由
        LambdaUpdateWrapper<Room> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Room::getId, roomId)
                .set(Room::getStatus, status);
        if (status == 0) {
            wrapper.set(Room::getAdminRemark, null);
        }
        boolean success = roomService.update(new Room(), wrapper);
        return success ? "房源状态更新成功" : "房源状态更新失败";
    }

    /**
     * 删除房源（软删除：status -> 5-已删除）
     * 仅房东本人（自己的房源）或管理员可操作；
     * 存在进行中订单（待支付/待入住/已入住/退租核算中）时禁止删除
     */
    @DeleteMapping("/delete/{roomId}")
    @RequiresRoles({0, 1})
    public Result<String> deleteRoom(HttpServletRequest request, @PathVariable Long roomId) {
        Integer userRole = (Integer) request.getAttribute("userRole");
        Long userId = (Long) request.getAttribute("realUserId");

        Room existRoom = roomService.getById(roomId);
        if (existRoom == null) {
            return Result.fail("房源不存在");
        }
        // 房东只能删除自己的房源
        if (userRole != null && userRole == 1
                && (existRoom.getLandlordId() == null || !existRoom.getLandlordId().equals(userId))) {
            return Result.fail("无权删除他人房源");
        }
        // 存在进行中订单时禁止删除，防止房态/订单数据异常
        long activeCount = roomOrderService.lambdaQuery()
                .eq(RoomOrder::getRoomId, roomId)
                .in(RoomOrder::getStatus, 0, 1, 2, 3)
                .count();
        if (activeCount > 0) {
            return Result.fail("该房源存在进行中的订单，无法删除，请先处理完订单再删除");
        }
        LambdaUpdateWrapper<Room> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Room::getId, roomId)
                .set(Room::getStatus, 5)
                .set(Room::getUpdateTime, LocalDateTime.now());
        boolean success = roomService.update(new Room(), wrapper);
        return success ? Result.success("房源已删除") : Result.fail("删除失败，请稍后重试");
    }

    /**
     * 房东查看自己名下房源的账单汇总（按房源统计）
     * 仅房东和管理员可调用
     */
    @GetMapping("/bill/rooms")
    @RequiresRoles({0, 1})
    public Result<List<RoomBillSummaryVO>> getLandlordBillSummary(HttpServletRequest request) {
        // 获取当前用户 ID 和角色
        Long currentUserId = (Long) request.getAttribute("realUserId");
        Integer role = (Integer) request.getAttribute("userRole");

        // 如果是管理员，可以查看所有房东的？但这里简化：管理员查看自己的（通常管理员没有房源），或者你可以扩展。
        // 更合理：如果 role=1 房东，查自己的；如果 role=0 管理员，可以查全部（但需要传入 landlordId 参数）。
        // 为了简单，我这里只支持房东查自己的。
        if (role == 0) {
            // 管理员：可以传参指定 landlordId，这里为了演示，直接返回提示
            return Result.fail("管理员请使用带 landlordId 参数的版本（暂未实现）");
        }

        // 房东查自己的（使用注入的 roomOrderService 调用实例方法）
        List<RoomBillSummaryVO> list = roomOrderService.getLandlordBillSummary(currentUserId);
        return Result.success(list);
    }
    /**
     * 个性化推荐（基于收藏标签）
     * 租客登录后调用
     */
    @GetMapping("/recommend")
    @RequiresRoles({2})
    public Result<List<Room>> recommendRooms(
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = (Long) request.getAttribute("realUserId");
        List<Room> rooms = roomService.recommendByFavorites(userId, limit);
        return Result.success(rooms);
    }

    /**
     * 热门房源推荐（公开，未登录用户也能看）
     */
    @GetMapping("/recommend/hot")
    public Result<List<Room>> recommendHotRooms(@RequestParam(defaultValue = "10") int limit) {
        List<Room> rooms = roomService.recommendHotRooms(limit);
        return Result.success(rooms);
    }

    /**
     * 房态日历接口（公开，游客/租客/房东/管理员均可查看）：
     * 返回指定房源在指定月份内每天的房态
     * @param roomId 房源ID
     * @param month 月份 yyyy-MM（可选，默认当前月）
     * days 中 status 含义：
     *   0-空闲 1-已预订(待支付/已支付待入住) 2-已入住 3-退租核算中
     *   并附带占用订单 orderId / orderNo 便于点击下钻
     */
    @GetMapping("/calendar/{roomId}")
    public Result<Map<String, Object>> getRoomCalendar(
            @PathVariable Long roomId,
            @RequestParam(required = false) String month) {
        Room room = roomService.getById(roomId);
        // 已删除（status=5）的房源视为不存在
        if (room == null || (room.getStatus() != null && room.getStatus() == 5)) {
            return Result.fail("房源不存在");
        }
        YearMonth ym;
        if (month == null || month.isBlank()) {
            ym = YearMonth.now();
        } else {
            try {
                ym = YearMonth.parse(month);
            } catch (Exception e) {
                return Result.fail("月份格式不正确，应为 yyyy-MM");
            }
        }
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        // 查询覆盖该月的有效订单（待支付/待入住/已入住/退租核算中）
        List<RoomOrder> orders = roomOrderService.lambdaQuery()
                .eq(RoomOrder::getRoomId, roomId)
                .le(RoomOrder::getCheckInDate, end)
                .ge(RoomOrder::getCheckOutDate, start)
                .in(RoomOrder::getStatus, 0, 1, 2, 3)
                .list();

        List<Map<String, Object>> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", d.toString());
            item.put("status", 0);
            for (RoomOrder o : orders) {
                LocalDate ci = o.getCheckInDate();
                LocalDate co = o.getCheckOutDate();
                if (ci == null || co == null) {
                    continue;
                }
                // 订单占用区间 [checkInDate, checkOutDate)
                if ((d.isEqual(ci) || d.isAfter(ci)) && d.isBefore(co)) {
                    Integer s = o.getStatus() == null ? 1 : o.getStatus();
                    // 待支付(0)在日历中也按"已预订"展示，避免日历显示空闲
                    int display = (s == 0) ? 1 : s;
                    int p = calendarStatusPriority(display);
                    int cur = (int) item.get("status");
                    if (p > calendarStatusPriority(cur)) {
                        item.put("status", display);
                        item.put("orderId", o.getId());
                        item.put("orderNo", o.getOrderNo());
                    }
                }
            }
            days.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("month", ym.toString());
        data.put("days", days);
        return Result.success(data);
    }

    private int calendarStatusPriority(Integer status) {
        if (status == null || status == 0) {
            return 0;  // 空闲
        }
        switch (status) {
            case 2: return 3;  // 已入住 优先级最高
            case 3: return 2;  // 退租核算中
            default: return 1; // 已预订（含待支付映射后的状态1）
        }
    }
}