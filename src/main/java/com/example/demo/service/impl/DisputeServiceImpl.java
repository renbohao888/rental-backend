package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.DisputeVO;
import com.example.demo.entity.Dispute;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomOrder;
import com.example.demo.entity.User;
import com.example.demo.mapper.DisputeMapper;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.mapper.RoomOrderMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.DisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DisputeServiceImpl extends ServiceImpl<DisputeMapper, Dispute> implements DisputeService {

    @Autowired
    private RoomOrderMapper roomOrderMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDispute(Long userId, Long orderId, String reason, String description, String evidenceImages) {
        // 1. 校验订单是否存在且属于该用户
        RoomOrder order = roomOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权对该订单发起纠纷");
        }
        // 仅允许已完成(status=4)或退租中(status=3)的订单发起纠纷
        if (order.getStatus() != 3 && order.getStatus() != 4) {
            throw new RuntimeException("当前订单状态不可发起纠纷");
        }

        // 2. 校验是否已发起过纠纷
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dispute::getOrderId, orderId);
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该订单已发起过纠纷，请勿重复提交");
        }

        // 3. 组装纠纷数据
        Dispute dispute = new Dispute();
        dispute.setOrderId(orderId);
        dispute.setUserId(userId);
        dispute.setRoomId(order.getRoomId());
        dispute.setReason(reason);
        dispute.setDescription(description);
        dispute.setEvidenceImages(evidenceImages);
        dispute.setStatus(0); // 待受理

        return this.save(dispute);
    }

    @Override
    public IPage<DisputeVO> getMyDisputes(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<Dispute> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dispute::getUserId, userId)
                .orderByDesc(Dispute::getCreateTime);
        if (status != null) {
            wrapper.eq(Dispute::getStatus, status);
        }
        IPage<Dispute> disputePage = this.page(page, wrapper);
        return convertToVOPage(disputePage);
    }

    @Override
    public IPage<DisputeVO> getAllDisputes(Integer pageNum, Integer pageSize, Integer status) {
        Page<Dispute> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Dispute::getCreateTime);
        if (status != null) {
            wrapper.eq(Dispute::getStatus, status);
        }
        IPage<Dispute> disputePage = this.page(page, wrapper);
        return convertToVOPage(disputePage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleDispute(Long disputeId, Integer status, String adminRemark, String resolution) {
        if (status < 1 || status > 3) {
            throw new RuntimeException("状态参数错误，仅支持1-处理中、2-已解决、3-已驳回");
        }

        Dispute dispute = this.getById(disputeId);
        if (dispute == null) {
            throw new RuntimeException("纠纷不存在");
        }
        if (dispute.getStatus() == 2 || dispute.getStatus() == 3) {
            throw new RuntimeException("该纠纷已处理完成，不可重复操作");
        }

        dispute.setStatus(status);
        dispute.setAdminRemark(adminRemark);
        dispute.setResolution(resolution);
        if (status == 2 || status == 3) {
            dispute.setHandleTime(LocalDateTime.now());
        }

        return this.updateById(dispute);
    }

    @Override
    public DisputeVO getDisputeDetail(Long disputeId) {
        Dispute dispute = this.getById(disputeId);
        if (dispute == null) {
            return null;
        }
        return convertToVO(dispute);
    }

    /**
     * 状态数字转文字
     */
    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待受理";
            case 1 -> "处理中";
            case 2 -> "已解决";
            case 3 -> "已驳回";
            default -> "未知";
        };
    }

    /**
     * 单个转换
     */
    private DisputeVO convertToVO(Dispute dispute) {
        DisputeVO vo = new DisputeVO();
        vo.setId(dispute.getId());
        vo.setOrderId(dispute.getOrderId());
        vo.setRoomId(dispute.getRoomId());
        vo.setUserId(dispute.getUserId());
        vo.setReason(dispute.getReason());
        vo.setDescription(dispute.getDescription());
        vo.setEvidenceImages(dispute.getEvidenceImages());
        vo.setStatus(dispute.getStatus());
        vo.setStatusText(getStatusText(dispute.getStatus()));
        vo.setAdminRemark(dispute.getAdminRemark());
        vo.setResolution(dispute.getResolution());
        vo.setHandleTime(dispute.getHandleTime());
        vo.setCreateTime(dispute.getCreateTime());

        // 补全订单号
        RoomOrder order = roomOrderMapper.selectById(dispute.getOrderId());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
        }

        // 补全房源信息
        Room room = roomMapper.selectById(dispute.getRoomId());
        if (room != null) {
            vo.setRoomTitle(room.getTitle());
            vo.setRoomCover(room.getCover());
        }

        // 补全用户信息
        User user = userMapper.selectById(dispute.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserPhone(user.getPhone());
        }

        return vo;
    }

    /**
     * 分页转换
     */
    private IPage<DisputeVO> convertToVOPage(IPage<Dispute> page) {
        List<Dispute> records = page.getRecords();
        if (records.isEmpty()) {
            Page<DisputeVO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            return emptyPage;
        }

        // 批量查询
        List<Long> orderIds = records.stream().map(Dispute::getOrderId).distinct().toList();
        List<Long> roomIds = records.stream().map(Dispute::getRoomId).distinct().toList();
        List<Long> userIds = records.stream().map(Dispute::getUserId).distinct().toList();

        Map<Long, RoomOrder> orderMap = roomOrderMapper.selectBatchIds(orderIds)
                .stream().collect(Collectors.toMap(RoomOrder::getId, o -> o));
        Map<Long, Room> roomMap = roomMapper.selectBatchIds(roomIds)
                .stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<DisputeVO> voList = records.stream().map(dispute -> {
            DisputeVO vo = convertToVO(dispute);
            // 手动填充关联对象
            RoomOrder order = orderMap.get(dispute.getOrderId());
            if (order != null) vo.setOrderNo(order.getOrderNo());
            Room room = roomMap.get(dispute.getRoomId());
            if (room != null) {
                vo.setRoomTitle(room.getTitle());
                vo.setRoomCover(room.getCover());
            }
            User user = userMap.get(dispute.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
                vo.setUserPhone(user.getPhone());
            }
            return vo;
        }).toList();

        Page<DisputeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}