package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.FriendVO;
import com.example.demo.dto.UserSearchVO;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.Friend;
import com.example.demo.entity.User;
import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.FriendMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    /** 查询 user_id -> friend_id 方向的关系记录 */
    private Friend findRelation(Long userId, Long friendId) {
        return this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .last("LIMIT 1"));
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        Friend a = findRelation(userId, friendId);
        Friend b = findRelation(friendId, userId);
        return (a != null && Integer.valueOf(1).equals(a.getStatus()))
                || (b != null && Integer.valueOf(1).equals(b.getStatus()));
    }

    @Override
    public UserSearchVO searchUser(String accountNo, Long currentUserId) {
        UserSearchVO vo = new UserSearchVO();
        if (accountNo == null || accountNo.trim().isEmpty()) {
            vo.setRelationship(-1);
            return vo;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getAccountNo, accountNo.trim())
                .last("LIMIT 1"));
        if (user == null) {
            vo.setRelationship(-1); // 用户不存在
            return vo;
        }
        vo.setId(user.getId());
        vo.setAccountNo(user.getAccountNo());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());

        if (user.getId().equals(currentUserId)) {
            vo.setRelationship(-2); // 是自己
            return vo;
        }
        Friend mine = findRelation(currentUserId, user.getId());
        Friend theirs = findRelation(user.getId(), currentUserId);
        if ((mine != null && Integer.valueOf(1).equals(mine.getStatus()))
                || (theirs != null && Integer.valueOf(1).equals(theirs.getStatus()))) {
            vo.setRelationship(1);
        } else if (mine != null && Integer.valueOf(0).equals(mine.getStatus())) {
            vo.setRelationship(2); // 已发送申请
        } else if (theirs != null && Integer.valueOf(0).equals(theirs.getStatus())) {
            vo.setRelationship(3); // 收到对方申请
        } else {
            vo.setRelationship(0);
        }
        return vo;
    }

    @Override
    public String sendRequest(Long currentUserId, Long friendId) {
        if (currentUserId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }
        User target = userMapper.selectById(friendId);
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }
        Friend mine = findRelation(currentUserId, friendId);
        Friend theirs = findRelation(friendId, currentUserId);
        if ((mine != null && Integer.valueOf(1).equals(mine.getStatus()))
                || (theirs != null && Integer.valueOf(1).equals(theirs.getStatus()))) {
            throw new RuntimeException("你们已经是好友了");
        }
        if (mine != null && Integer.valueOf(0).equals(mine.getStatus())) {
            throw new RuntimeException("已发送过好友申请，请等待对方处理");
        }
        if (theirs != null && Integer.valueOf(0).equals(theirs.getStatus())) {
            // 对方之前申请过我 → 自动成为好友
            theirs.setStatus(1);
            this.updateById(theirs);
            return "对方已向你发送过申请，已自动成为好友";
        }
        if (mine != null && Integer.valueOf(2).equals(mine.getStatus())) {
            // 之前被拒绝过，重新申请
            mine.setStatus(0);
            this.updateById(mine);
            return "好友申请已重新发送";
        }
        Friend friend = new Friend();
        friend.setUserId(currentUserId);
        friend.setFriendId(friendId);
        friend.setStatus(0);
        this.save(friend);
        return "好友申请已发送";
    }

    @Override
    public List<FriendVO> getFriendList(Long currentUserId) {
        // 我发出的好友关系
        List<Friend> outgoing = this.list(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, currentUserId)
                .eq(Friend::getStatus, 1));
        // 对方发出、我接受的关系
        List<Friend> incoming = this.list(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getFriendId, currentUserId)
                .eq(Friend::getStatus, 1));

        Map<Long, Friend> relationMap = new HashMap<>();
        for (Friend f : outgoing) {
            relationMap.put(f.getFriendId(), f);
        }
        for (Friend f : incoming) {
            relationMap.putIfAbsent(f.getUserId(), f);
        }

        List<FriendVO> result = new ArrayList<>();
        for (Map.Entry<Long, Friend> entry : relationMap.entrySet()) {
            Long friendUserId = entry.getKey();
            User user = userMapper.selectById(friendUserId);
            if (user == null) {
                continue;
            }
            FriendVO vo = new FriendVO();
            vo.setRequestId(entry.getValue().getId());
            vo.setId(user.getId());
            vo.setAccountNo(user.getAccountNo());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setRole(user.getRole());

            // 最近一条聊天记录
            ChatMessage last = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .and(w -> w.eq(ChatMessage::getFromUserId, currentUserId).eq(ChatMessage::getToUserId, friendUserId)
                            .or().eq(ChatMessage::getFromUserId, friendUserId).eq(ChatMessage::getToUserId, currentUserId))
                    .orderByDesc(ChatMessage::getId)
                    .last("LIMIT 1"));
            if (last != null) {
                vo.setLastMessage(last.getContent());
                vo.setLastMessageTime(last.getCreateTime());
            }

            // 未读消息数（对方发给我的未读）
            Long unread = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getToUserId, currentUserId)
                    .eq(ChatMessage::getFromUserId, friendUserId)
                    .eq(ChatMessage::getIsRead, 0));
            vo.setUnreadCount(unread.intValue());
            result.add(vo);
        }

        // 按最近消息时间倒序（有消息的排前面）
        result.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) {
                return b.getId().compareTo(a.getId());
            }
            if (a.getLastMessageTime() == null) {
                return 1;
            }
            if (b.getLastMessageTime() == null) {
                return -1;
            }
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });
        return result;
    }

    @Override
    public List<FriendVO> getPendingRequests(Long currentUserId) {
        List<Friend> rows = this.list(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getFriendId, currentUserId)
                .eq(Friend::getStatus, 0)
                .orderByDesc(Friend::getCreateTime));
        List<FriendVO> result = new ArrayList<>();
        for (Friend f : rows) {
            User user = userMapper.selectById(f.getUserId());
            if (user == null) {
                continue;
            }
            FriendVO vo = new FriendVO();
            vo.setRequestId(f.getId());
            vo.setId(user.getId());
            vo.setAccountNo(user.getAccountNo());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setRole(user.getRole());
            vo.setRequestTime(f.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    public boolean handleRequest(Long currentUserId, Long requestId, boolean accept) {
        Friend friend = this.getById(requestId);
        if (friend == null || friend.getStatus() == null || friend.getStatus() != 0) {
            return false;
        }
        // 只有被申请方本人能处理
        if (!friend.getFriendId().equals(currentUserId)) {
            return false;
        }
        if (accept) {
            friend.setStatus(1);
            return this.updateById(friend);
        }
        // 拒绝 → 删除该申请记录
        return this.removeById(friend.getId());
    }

    @Override
    public boolean removeFriend(Long currentUserId, Long friendId) {
        Friend a = findRelation(currentUserId, friendId);
        Friend b = findRelation(friendId, currentUserId);
        boolean ok = true;
        if (a != null) {
            ok = this.removeById(a.getId()) && ok;
        }
        if (b != null) {
            ok = this.removeById(b.getId()) && ok;
        }
        return ok;
    }
}
