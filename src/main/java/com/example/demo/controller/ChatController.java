package com.example.demo.controller;

import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.ChatSendDTO;
import com.example.demo.entity.ChatMessage;
import com.example.demo.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/chat")
@RequiresRoles({0, 1, 2})
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 发送聊天消息（双方必须是好友）
     */
    @PostMapping("/send")
    public Result<String> send(@RequestBody ChatSendDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        boolean ok = chatService.sendMessage(userId, dto.getToUserId(), dto.getContent());
        return ok ? Result.success("发送成功") : Result.fail("发送失败，请稍后重试");
    }

    /**
     * 与某好友的聊天记录
     */
    @GetMapping("/history")
    public Result<List<ChatMessage>> history(@RequestParam Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        return Result.success(chatService.getHistory(userId, friendId));
    }

    /**
     * 当前用户未读消息总数（导航栏红点用）
     */
    @GetMapping("/unread/count")
    public Result<Integer> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        return Result.success(chatService.getUnreadCount(userId));
    }

    /**
     * 将某好友发来的未读消息标记为已读
     */
    @PostMapping("/read")
    public Result<String> markRead(@RequestParam Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("realUserId");
        chatService.markRead(userId, friendId);
        return Result.success("ok");
    }
}
