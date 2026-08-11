package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.dto.MessageVO;
import com.example.demo.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/message")
@RequiresRoles({2})
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 分页查询我的消息列表
     */
    @GetMapping("/list")
    public Result<IPage<MessageVO>> listMessages(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type) {
        try {
            Long userId = (Long) request.getAttribute("realUserId");
            IPage<MessageVO> page = messageService.getMessageList(userId, pageNum, pageSize, type);
            return Result.success(page);
        } catch (Exception e) {
            // 消息表可能还未创建，返回空数据
            IPage<MessageVO> emptyPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
            emptyPage.setRecords(java.util.Collections.emptyList());
            return Result.success(emptyPage);
        }
    }

    /**
     * 分享房源给指定手机号的租客
     */
    @PostMapping("/share")
    public Result<String> shareRoom(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long senderId = (Long) request.getAttribute("realUserId");
        Long roomId = params.get("roomId") == null ? null : Long.valueOf(params.get("roomId").toString());
        String recipientPhone = params.get("recipientPhone") == null ? null : params.get("recipientPhone").toString();
        String message = params.get("message") == null ? null : params.get("message").toString();
        try {
            boolean success = messageService.shareRoom(senderId, roomId, recipientPhone, message);
            return success ? Result.success("分享成功") : Result.fail("分享失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    public Result<String> deleteMessage(HttpServletRequest request, @PathVariable Long messageId) {
        Long userId = (Long) request.getAttribute("realUserId");
        try {
            boolean success = messageService.deleteMessage(messageId, userId);
            return success ? Result.success("删除成功") : Result.fail("删除失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
