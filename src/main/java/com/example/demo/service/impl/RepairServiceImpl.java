package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.SupervisionStatsVO;
import com.example.demo.entity.Dispute;
import com.example.demo.entity.Repair;
import com.example.demo.entity.Room;
import com.example.demo.mapper.DisputeMapper;
import com.example.demo.mapper.RepairMapper;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {

    @Autowired
    private RoomMapper roomMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitRepair(Long userId, Long roomId, String title, String description, List<String> imageUrls) {
        // 1. 校验房源是否存在
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new RuntimeException("房源不存在");
        }

        // 2. 组装报修数据
        Repair repair = new Repair();
        repair.setUserId(userId);
        repair.setRoomId(roomId);
        repair.setTitle(title);
        repair.setDescription(description);
        repair.setStatus(0); // 待处理

        // 3. 图片URL列表转JSON字符串
        if (imageUrls != null && !imageUrls.isEmpty()) {
            // 简单存储为逗号分隔字符串（也可以用JSON，这里用逗号分隔）
            repair.setImages(String.join(",", imageUrls));
        }

        return this.save(repair);
    }

    @Override
    public IPage<Repair> getMyRepairs(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<Repair> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime);
        if (status != null) {
            wrapper.eq(Repair::getStatus, status);
        }
        return this.page(page, wrapper);
    }

    @Override
    public IPage<Repair> getLandlordRepairs(Long landlordId, Integer pageNum, Integer pageSize, Integer status) {
        // 1. 先查出该房东的所有房源ID
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getLandlordId, landlordId)
                .select(Room::getId);
        List<Long> roomIds = roomMapper.selectList(roomWrapper)
                .stream()
                .map(Room::getId)
                .toList();

        if (roomIds.isEmpty()) {
            return new Page<>(pageNum, pageSize); // 返回空分页
        }

        // 2. 查询这些房源下的报修
        Page<Repair> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Repair::getRoomId, roomIds)
                .orderByDesc(Repair::getCreateTime);
        if (status != null) {
            wrapper.eq(Repair::getStatus, status);
        }
        return this.page(page, wrapper);
    }

    @Override
    public IPage<Repair> getAllRepairs(Integer pageNum, Integer pageSize, Integer status) {
        Page<Repair> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Repair::getCreateTime);
        if (status != null) {
            wrapper.eq(Repair::getStatus, status);
        }
        return this.page(page, wrapper);
    }

    @Override
    public Repair getRepairDetail(Long repairId) {
        return this.getById(repairId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleRepair(Long repairId, Long handlerId, Integer status, String handlerRemark) {
        // 1. 校验状态是否合法（仅允许 1-处理中，2-已完成，3-已关闭）
        if (status < 1 || status > 3) {
            throw new RuntimeException("状态参数错误，仅支持1-处理中、2-已完成、3-已关闭");
        }

        Repair repair = this.getById(repairId);
        if (repair == null) {
            throw new RuntimeException("报修记录不存在");
        }

        // 2. 如果状态变为已完成或已关闭，记录处理完成时间
        repair.setStatus(status);
        repair.setHandlerId(handlerId);
        repair.setHandlerRemark(handlerRemark);
        if (status == 2 || status == 3) {
            repair.setHandleTime(LocalDateTime.now());
        }

        return this.updateById(repair);
    }

    @Autowired
    private DisputeMapper disputeMapper;  // 👈 新增注入

    @Override
    public SupervisionStatsVO getSupervisionStats() {
        SupervisionStatsVO stats = new SupervisionStatsVO();

        // 1. 报修统计
        List<Repair> allRepairs = this.list();
        stats.setTotalRepairs(allRepairs.size());
        stats.setPendingRepairs((int) allRepairs.stream().filter(r -> r.getStatus() == 0).count());
        stats.setProcessingRepairs((int) allRepairs.stream().filter(r -> r.getStatus() == 1).count());
        stats.setCompletedRepairs((int) allRepairs.stream().filter(r -> r.getStatus() == 2 || r.getStatus() == 3).count());

        // 2. 纠纷统计
        List<Dispute> allDisputes = disputeMapper.selectList(null);
        stats.setTotalDisputes(allDisputes.size());
        stats.setPendingDisputes((int) allDisputes.stream().filter(d -> d.getStatus() == 0).count());
        stats.setProcessingDisputes((int) allDisputes.stream().filter(d -> d.getStatus() == 1).count());
        stats.setResolvedDisputes((int) allDisputes.stream().filter(d -> d.getStatus() == 2).count());
        stats.setRejectedDisputes((int) allDisputes.stream().filter(d -> d.getStatus() == 3).count());

        return stats;
    }

    @Override
    public boolean cancelRepair(Long userId, Long repairId) {
        Repair repair = this.getById(repairId);
        if (repair == null) {
            throw new RuntimeException("报修记录不存在");
        }
        if (!repair.getUserId().equals(userId)) {
            throw new RuntimeException("只能撤销自己的报修");
        }
        if (repair.getStatus() != 0) {
            throw new RuntimeException("只能撤销待处理的报修");
        }
        repair.setStatus(3); // 已关闭
        return this.updateById(repair);
    }
}