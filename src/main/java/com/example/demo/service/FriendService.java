package com.example.demo.service;

import com.example.demo.dto.FriendVO;
import com.example.demo.dto.UserSearchVO;

import java.util.List;

public interface FriendService {

    /** 按账号搜索用户 */
    UserSearchVO searchUser(String accountNo, Long currentUserId);

    /** 发送好友申请 */
    String sendRequest(Long currentUserId, Long friendId);

    /** 好友列表（含最近消息、未读数） */
    List<FriendVO> getFriendList(Long currentUserId);

    /** 收到的好友申请列表 */
    List<FriendVO> getPendingRequests(Long currentUserId);

    /** 处理好友申请（accept=true 接受，false 拒绝） */
    boolean handleRequest(Long currentUserId, Long requestId, boolean accept);

    /** 删除好友（双向解除） */
    boolean removeFriend(Long currentUserId, Long friendId);

    /** 判断两人是否为好友 */
    boolean isFriend(Long userId, Long friendId);
}
