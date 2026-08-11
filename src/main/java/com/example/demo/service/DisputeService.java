package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.DisputeVO;

public interface DisputeService {

    /**
     * 租客发起纠纷
     */
    boolean submitDispute(Long userId, Long orderId, String reason, String description, String evidenceImages);

    /**
     * 租客查询自己的纠纷列表
     */
    IPage<DisputeVO> getMyDisputes(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员查询所有纠纷列表
     */
    IPage<DisputeVO> getAllDisputes(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员处理纠纷（更新状态和处理备注）
     */
    boolean handleDispute(Long disputeId, Integer status, String adminRemark, String resolution);

    /**
     * 查询纠纷详情
     */
    DisputeVO getDisputeDetail(Long disputeId);
}