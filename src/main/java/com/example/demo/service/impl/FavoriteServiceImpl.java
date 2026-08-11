package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.Room;
import com.example.demo.mapper.FavoriteMapper;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Autowired
    private RoomMapper roomMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFavorite(Long userId, Long roomId) {
        // 1. 校验房源是否存在且已上架
        Room room = roomMapper.selectById(roomId);
        if (room == null || room.getStatus() != 1) {
            throw new RuntimeException("房源不存在或已下架，无法收藏");
        }

        // 2. 检查是否已收藏（直接查表，排除逻辑删除）
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getRoomId, roomId);
        Favorite exist = this.getOne(wrapper);
        if (exist != null) {
            throw new RuntimeException("您已收藏该房源，请勿重复收藏");
        }

        // 3. 新增收藏（去掉 isDeleted 手动设置，由数据库默认值处理）
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setRoomId(roomId);
        favorite.setCreateTime(LocalDateTime.now());
        return this.save(favorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelFavorite(Long userId, Long roomId) {
        // 🔥 物理删除：调用自定义 Mapper 方法，彻底删掉记录
        int deleted = baseMapper.physicalDeleteByUserIdAndRoomId(userId, roomId);
        if (deleted == 0) {
            throw new RuntimeException("您尚未收藏该房源");
        }
        return true;
    }

    @Override
    public IPage<Favorite> getMyFavorites(Long userId, Integer pageNum, Integer pageSize) {
        Page<Favorite> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public IPage<Room> getFavoriteRoomPage(Long userId, Integer pageNum, Integer pageSize, String sortBy) {
        List<Room> rooms = baseMapper.selectFavoriteRooms(userId);

        // 按前端排序字段做内存排序（避免 SQL 注入）
        if (sortBy != null) {
            switch (sortBy) {
                case "price-asc" -> rooms.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
                case "price-desc" -> rooms.sort((a, b) -> b.getPrice().compareTo(a.getPrice()));
                case "rating" -> rooms.sort((a, b) -> {
                    BigDecimal ar = a.getRating() == null ? BigDecimal.ZERO : a.getRating();
                    BigDecimal br = b.getRating() == null ? BigDecimal.ZERO : b.getRating();
                    return br.compareTo(ar);
                });
                default -> { /* newest：保持按收藏时间倒序 */ }
            }
        }

        long total = rooms.size();
        Page<Room> page = new Page<>(pageNum, pageSize, total);
        int from = (int) ((pageNum - 1) * pageSize);
        if (from >= rooms.size()) {
            page.setRecords(List.of());
            return page;
        }
        int to = Math.min((int) (pageNum * pageSize), rooms.size());
        page.setRecords(new ArrayList<>(rooms.subList(from, to)));
        return page;
    }

    @Override
    public boolean isFavorited(Long userId, Long roomId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getRoomId, roomId);
        return this.count(wrapper) > 0;
    }
}