package com.example.demo.service;

import com.example.demo.entity.ChatMessage;

import java.util.List;

public interface ChatService {

    /** 发送聊天消息（双方必须是好友） */
    boolean sendMessage(Long fromUserId, Long toUserId, String content);

    /** 与某好友的聊天记录（时间正序，最多200条） */
    List<ChatMessage> getHistory(Long userId, Long friendId);

    /** 当前用户未读消息总数 */
    int getUnreadCount(Long userId);

    /** 将与某好友的未读消息标记为已读 */
    void markRead(Long userId, Long friendId);
}
