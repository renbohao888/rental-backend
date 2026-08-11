package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.EvaluationVO;
import com.example.demo.entity.Evaluation;

import java.util.List;

public interface EvaluationService {

    /**
     * 租客提交评价（一个订单只能评价一次）
     */
    boolean submitEvaluation(Long userId, Long orderId, Integer rating, String content, List<String> imageUrls);

    /**
     * 房东回复评价
     */
    boolean replyEvaluation(Long evaluationId, Long landlordId, String replyContent);

    /**
     * 租客查询自己的评价列表
     */
    IPage<EvaluationVO> getMyEvaluations(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 房东查询自己房源的评价列表
     */
    IPage<EvaluationVO> getLandlordEvaluations(Long landlordId, Integer pageNum, Integer pageSize);

    /**
     * 管理员查询所有评价列表
     */
    IPage<EvaluationVO> getAllEvaluations(Integer pageNum, Integer pageSize);

    /**
     * 查询房源的评价列表（公开，用于前端展示）
     */
    IPage<EvaluationVO> getRoomEvaluations(Long roomId, Integer pageNum, Integer pageSize);

    /**
     * 查询评价详情
     */
    Evaluation getEvaluationDetail(Long evaluationId);

    /**
     * 删除评价（管理员）
     */
    boolean deleteEvaluation(Long evaluationId);
}