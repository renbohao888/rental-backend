package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友列表/好友申请 返回对象
 */
@Data
public class FriendVO {

    /** 好友关系记录ID（处理好友申请时使用） */
    private Long requestId;

    /** 好友用户ID */
    private Long id;

    /** 系统账号 */
    private String accountNo;

    private String nickname;

    private String avatar;

    /** 0-管理员 1-房东 2-租客 */
    private Integer role;

    /** 最近一条聊天消息 */
    private String lastMessage;

    /** 最近一条消息时间 */
    private LocalDateTime lastMessageTime;

    /** 未读消息数 */
    private Integer unreadCount;

    /** 好友申请发起时间（好友申请列表使用） */
    private LocalDateTime requestTime;
}
