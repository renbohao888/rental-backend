package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.MessageVO;
import com.example.demo.entity.Message;

public interface MessageService extends IService<Message> {

    /**
     * 分页查询某用户的消息列表
     */
    IPage<MessageVO> getMessageList(Long userId, Integer pageNum, Integer pageSize, String type);

    /**
     * 分享房源给指定手机号的租客
     */
    boolean shareRoom(Long senderId, Long roomId, String recipientPhone, String message);

    /**
     * 删除消息（仅属主可删）
     */
    boolean deleteMessage(Long messageId, Long userId);

    /**
     * 统计未读消息数
     */
    long countUnread(Long userId);
}
