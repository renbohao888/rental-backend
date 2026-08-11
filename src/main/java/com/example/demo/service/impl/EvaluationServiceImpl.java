package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.EvaluationVO;
import com.example.demo.entity.Evaluation;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomOrder;
import com.example.demo.entity.User;
import com.example.demo.mapper.EvaluationMapper;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.mapper.RoomOrderMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EvaluationServiceImpl extends ServiceImpl<EvaluationMapper, Evaluation> implements EvaluationService {

    @Autowired
    private RoomOrderMapper roomOrderMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitEvaluation(Long userId, Long orderId, Integer rating, String content, List<String> imageUrls) {
        // 1. 校验评分范围
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }

        // 2. 校验订单是否存在且已完成
        RoomOrder order = roomOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 4) {
            throw new RuntimeException("只有已完成的订单才能评价");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权评价他人的订单");
        }

        // 3. 校验是否已评价过该订单
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Evaluation::getOrderId, orderId);
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该订单已评价，不可重复评价");
        }

        // 4. 组装评价数据
        Evaluation evaluation = new Evaluation();
        evaluation.setOrderId(orderId);
        evaluation.setUserId(userId);
        evaluation.setRoomId(order.getRoomId());
        evaluation.setRating(rating);
        evaluation.setContent(content);
        if (imageUrls != null && !imageUrls.isEmpty()) {
            evaluation.setImages(String.join(",", imageUrls));
        }

        return this.save(evaluation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replyEvaluation(Long evaluationId, Long landlordId, String replyContent) {
        Evaluation evaluation = this.getById(evaluationId);
        if (evaluation == null) {
            throw new RuntimeException("评价不存在");
        }

        // 校验房源是否属于该房东
        Room room = roomMapper.selectById(evaluation.getRoomId());
        if (room == null || !room.getLandlordId().equals(landlordId)) {
            throw new RuntimeException("无权回复他人房源的评价");
        }

        evaluation.setReplyContent(replyContent);
        evaluation.setReplyTime(LocalDateTime.now());
        return this.updateById(evaluation);
    }

    @Override
    public IPage<EvaluationVO> getMyEvaluations(Long userId, Integer pageNum, Integer pageSize) {
        Page<Evaluation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Evaluation::getUserId, userId)
                .orderByDesc(Evaluation::getCreateTime);
        IPage<Evaluation> evaluationPage = this.page(page, wrapper);
        return convertToVOPage(evaluationPage);
    }

    @Override
    public IPage<EvaluationVO> getLandlordEvaluations(Long landlordId, Integer pageNum, Integer pageSize) {
        // 1. 查出该房东的所有房源ID
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getLandlordId, landlordId)
                .select(Room::getId);
        List<Long> roomIds = roomMapper.selectList(roomWrapper)
                .stream()
                .map(Room::getId)
                .toList();

        if (roomIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        // 2. 查询这些房源的评价
        Page<Evaluation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Evaluation::getRoomId, roomIds)
                .orderByDesc(Evaluation::getCreateTime);
        IPage<Evaluation> evaluationPage = this.page(page, wrapper);
        return convertToVOPage(evaluationPage);
    }

    @Override
    public IPage<EvaluationVO> getAllEvaluations(Integer pageNum, Integer pageSize) {
        Page<Evaluation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Evaluation::getCreateTime);
        IPage<Evaluation> evaluationPage = this.page(page, wrapper);
        return convertToVOPage(evaluationPage);
    }

    @Override
    public IPage<EvaluationVO> getRoomEvaluations(Long roomId, Integer pageNum, Integer pageSize) {
        Page<Evaluation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Evaluation::getRoomId, roomId)
                .orderByDesc(Evaluation::getCreateTime);
        IPage<Evaluation> evaluationPage = this.page(page, wrapper);
        return convertToVOPage(evaluationPage);
    }

    @Override
    public Evaluation getEvaluationDetail(Long evaluationId) {
        return this.getById(evaluationId);
    }

    @Override
    public boolean deleteEvaluation(Long evaluationId) {
        return this.removeById(evaluationId);
    }

    /**
     * 将 Evaluation 分页转换为 EvaluationVO 分页（包含房源标题、用户昵称等）
     */
    private IPage<EvaluationVO> convertToVOPage(IPage<Evaluation> page) {
        List<Evaluation> records = page.getRecords();
        if (records.isEmpty()) {
            Page<EvaluationVO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            return emptyPage;
        }

        // 提取所有用户ID和房源ID
        List<Long> userIds = records.stream().map(Evaluation::getUserId).distinct().toList();
        List<Long> roomIds = records.stream().map(Evaluation::getRoomId).distinct().toList();

        // 批量查询用户和房源
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Room> roomMap = roomMapper.selectBatchIds(roomIds)
                .stream().collect(Collectors.toMap(Room::getId, r -> r));

        // 转换为VO
        List<EvaluationVO> voList = records.stream().map(eval -> {
            EvaluationVO vo = new EvaluationVO();
            vo.setId(eval.getId());
            vo.setOrderId(eval.getOrderId());
            vo.setRoomId(eval.getRoomId());
            vo.setRating(eval.getRating());
            vo.setContent(eval.getContent());
            vo.setImages(eval.getImages());
            vo.setReplyContent(eval.getReplyContent());
            vo.setReplyTime(eval.getReplyTime());
            vo.setCreateTime(eval.getCreateTime());

            User user = userMap.get(eval.getUserId());
            if (user != null) {
                vo.setUserId(user.getId());
                vo.setUserNickname(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }

            Room room = roomMap.get(eval.getRoomId());
            if (room != null) {
                vo.setRoomTitle(room.getTitle());
                vo.setRoomCover(room.getCover());
            }

            return vo;
        }).toList();

        Page<EvaluationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}