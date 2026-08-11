package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.ChatMessage;
import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.service.ChatService;
import com.example.demo.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {

    @Autowired
    private FriendService friendService;

    @Override
    public boolean sendMessage(Long fromUserId, Long toUserId, String content) {
        if (toUserId == null) {
            throw new RuntimeException("接收人不能为空");
        }
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能给自己发消息");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("消息内容不能为空");
        }
        if (!friendService.isFriend(fromUserId, toUserId)) {
            throw new RuntimeException("请先添加对方为好友再聊天");
        }
        ChatMessage msg = new ChatMessage();
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setContent(content);
        msg.setIsRead(0);
        return this.save(msg);
    }

    @Override
    public List<ChatMessage> getHistory(Long userId, Long friendId) {
        return this.list(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromUserId, userId).eq(ChatMessage::getToUserId, friendId)
                        .or().eq(ChatMessage::getFromUserId, friendId).eq(ChatMessage::getToUserId, userId))
                .orderByAsc(ChatMessage::getId)
                .last("LIMIT 200"));
    }

    @Override
    public int getUnreadCount(Long userId) {
        Long count = this.count(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getIsRead, 0));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void markRead(Long userId, Long friendId) {
        this.update(new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getFromUserId, friendId)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1));
    }
}
