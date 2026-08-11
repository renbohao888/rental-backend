package com.example.demo.dto;

import lombok.Data;

/**
 * 按账号搜索用户的结果
 */
@Data
public class UserSearchVO {

    private Long id;

    /** 系统账号 */
    private String accountNo;

    private String nickname;

    private String avatar;

    /** 0-管理员 1-房东 2-租客 */
    private Integer role;

    /**
     * 与当前用户的关系：
     * -1-用户不存在
     * -2-是自己
     * 0-非好友
     * 1-已是好友
     * 2-已发送好友申请（等待对方处理）
     * 3-收到对方的好友申请
     */
    private Integer relationship;
}
