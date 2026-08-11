package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.Repair;
import com.example.demo.entity.RoomOrder;
import com.example.demo.mapper.FavoriteMapper;
import com.example.demo.mapper.RepairMapper;
import com.example.demo.service.MessageService;
import com.example.demo.service.RoomOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/tenant")
@RequiresRoles({2})
public class TenantController {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private RepairMapper repairMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private RoomOrderService roomOrderService;

    /**
     * 租客个人中心统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");

        Map<String, Object> data = new HashMap<>();

        // 收藏数
        LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(Favorite::getUserId, userId);
        data.put("favoriteCount", favoriteMapper.selectCount(favoriteWrapper));

        // 待处理报修数（status=0）
        LambdaQueryWrapper<Repair> repairWrapper = new LambdaQueryWrapper<>();
        repairWrapper.eq(Repair::getUserId, userId).eq(Repair::getStatus, 0);
        data.put("repairCount", repairMapper.selectCount(repairWrapper));

        // 未读消息数（消息表可能未创建，容错处理）
        try {
            data.put("messageCount", messageService.countUnread(userId));
        } catch (Exception e) {
            data.put("messageCount", 0);
        }

        // 活跃订单数（0待支付 1已支付 2已入住 3退租核算）
        LambdaQueryWrapper<RoomOrder> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(RoomOrder::getUserId, userId).in(RoomOrder::getStatus, 0, 1, 2, 3);
        data.put("activeOrderCount", roomOrderService.count(activeWrapper));

        // 完成订单数（status=4）
        LambdaQueryWrapper<RoomOrder> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(RoomOrder::getUserId, userId).eq(RoomOrder::getStatus, 4);
        data.put("completedOrderCount", roomOrderService.count(completedWrapper));

        return Result.success(data);
    }
}
