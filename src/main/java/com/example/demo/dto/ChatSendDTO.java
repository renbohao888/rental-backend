package com.example.demo.dto;

import lombok.Data;

/**
 * 发送聊天消息
 */
@Data
public class ChatSendDTO {

    /** 接收方用户ID */
    private Long toUserId;

    /** 消息内容 */
    private String content;
}
