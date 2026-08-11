package com.example.demo.controller;

import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.FriendVO;
import com.example.demo.dto.UserSearchVO;
import com.example.demo.service.FriendService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/friend")
@RequiresRoles({0, 1, 2})
public class FriendController {

    @Autowired
    private FriendService friendService;

    /**
     * 按系统账号搜索用户
     */
    @GetMapping("/search")
    public Result<UserSearchVO> search(@RequestParam String account, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        return Result.success(friendService.searchUser(account, userId));
    }

    /**
     * 发送好友申请
     */
    @PostMapping("/request")
    public Result<String> sendRequest(@RequestParam Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        String msg = friendService.sendRequest(userId, friendId);
        return Result.success(msg);
    }

    /**
     * 好友列表（含最近消息、未读数）
     */
    @GetMapping("/list")
    public Result<List<FriendVO>> friendList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        return Result.success(friendService.getFriendList(userId));
    }

    /**
     * 收到的好友申请列表
     */
    @GetMapping("/requests")
    public Result<List<FriendVO>> requests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        return Result.success(friendService.getPendingRequests(userId));
    }

    /**
     * 处理好友申请（accept=true 接受，false 拒绝）
     */
    @PostMapping("/handle")
    public Result<String> handle(@RequestParam Long requestId,
                                 @RequestParam Boolean accept,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        boolean ok = friendService.handleRequest(userId, requestId, accept);
        return ok ? Result.success(accept ? "已接受好友申请" : "已拒绝好友申请")
                : Result.fail("操作失败，申请不存在或已被处理");
    }

    /**
     * 删除好友（双向解除）
     */
    @DeleteMapping("/remove/{friendId}")
    public Result<String> remove(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        boolean ok = friendService.removeFriend(userId, friendId);
        return ok ? Result.success("已删除好友") : Result.fail("删除失败");
    }
}
