package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.SupervisionStatsVO;
import com.example.demo.entity.Repair;

import java.util.List;

public interface RepairService {

    /**
     * 租客提交报修（含图片上传）
     */
    boolean submitRepair(Long userId, Long roomId, String title, String description, List<String> imageUrls);

    /**
     * 租客查询自己的报修列表
     */
    IPage<Repair> getMyRepairs(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 房东查询自己房源下的报修列表
     */
    IPage<Repair> getLandlordRepairs(Long landlordId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员查询所有报修列表
     */
    IPage<Repair> getAllRepairs(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 查询报修详情
     */
    Repair getRepairDetail(Long repairId);

    /**
     * 房东/管理员处理报修（更新状态和处理备注）
     */
    boolean handleRepair(Long repairId, Long handlerId, Integer status, String handlerRemark);

    /**
     * 管理员获取报修督办统计数据
     */
    SupervisionStatsVO getSupervisionStats();

    /**
     * 租客撤销报修（仅待处理状态可撤销）
     */
    boolean cancelRepair(Long userId, Long repairId);
}