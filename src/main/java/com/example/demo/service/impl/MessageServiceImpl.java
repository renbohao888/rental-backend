package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.MessageVO;
import com.example.demo.entity.Message;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import com.example.demo.mapper.MessageMapper;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Override
    public IPage<MessageVO> getMessageList(Long userId, Integer pageNum, Integer pageSize, String type) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId)
                .orderByDesc(Message::getCreateTime);
        if (type != null && !type.isBlank()) {
            wrapper.eq(Message::getType, type);
        }
        IPage<Message> messagePage = this.page(page, wrapper);
        return convertToVOPage(messagePage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean shareRoom(Long senderId, Long roomId, String recipientPhone, String message) {
        if (recipientPhone == null || recipientPhone.trim().isEmpty()) {
            throw new RuntimeException("接收者手机号不能为空");
        }
        if (roomId == null) {
            throw new RuntimeException("房源ID不能为空");
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new RuntimeException("房源不存在");
        }
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getPhone, recipientPhone).eq(User::getRole, 2);
        User target = userMapper.selectOne(userWrapper);
        if (target == null) {
            throw new RuntimeException("未找到该手机号对应的租客");
        }
        Message msg = new Message();
        msg.setType("share");
        msg.setContent(message == null || message.isBlank()
                ? "向你分享了房源【" + room.getTitle() + "】"
                : message);
        msg.setSenderId(senderId);
        msg.setUserId(target.getId());
        msg.setRelationId(roomId);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        return this.save(msg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMessage(Long messageId, Long userId) {
        Message message = this.getById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }
        if (!message.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该消息");
        }
        return this.removeById(messageId);
    }

    @Override
    public long countUnread(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, 0);
        return this.count(wrapper);
    }

    private MessageVO convertToVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setType(message.getType());
        vo.setContent(message.getContent());
        vo.setSenderId(message.getSenderId());
        vo.setRelationId(message.getRelationId());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());

        if (message.getSenderId() != null) {
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
        }

        // 分享消息附带房源信息
        if ("share".equals(message.getType()) && message.getRelationId() != null) {
            Room room = roomMapper.selectById(message.getRelationId());
            if (room != null) {
                MessageVO.RoomInfo info = new MessageVO.RoomInfo();
                info.setId(room.getId());
                info.setTitle(room.getTitle());
                info.setCover(room.getCover());
                info.setPrice(room.getPrice());
                vo.setRoomInfo(info);
            }
        }
        return vo;
    }

    private IPage<MessageVO> convertToVOPage(IPage<Message> page) {
        List<Message> records = page.getRecords();
        Page<MessageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (records == null || records.isEmpty()) {
            return voPage;
        }
        List<Long> senderIds = records.stream()
                .map(Message::getSenderId)
                .filter(l -> l != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = senderIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(senderIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        voPage.setRecords(records.stream().map(msg -> {
            MessageVO vo = convertToVO(msg);
            if (msg.getSenderId() != null) {
                User sender = userMap.get(msg.getSenderId());
                if (sender != null) {
                    vo.setSenderName(sender.getNickname());
                    vo.setSenderAvatar(sender.getAvatar());
                }
            }
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }
}
