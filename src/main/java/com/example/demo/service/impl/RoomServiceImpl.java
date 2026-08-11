package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.Room;
import com.example.demo.mapper.FavoriteMapper;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    private FavoriteMapper favoriteMapper;

    // 构造函数注入
    public RoomServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // ===== 房源预定（分布式锁） =====
    @Override
    public boolean bookRoom(Long roomId, Long userId) {
        String lockKey = "lock:room:" + roomId;
        try {
            boolean lockSuccess = Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, userId.toString(), Duration.ofSeconds(5)));
            if (!lockSuccess) {
                return false;
            }
            Room room = this.getById(roomId);
            if (room == null || room.getStatus() != 1) {
                return false;
            }
            room.setStatus(2);
            return this.updateById(room);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    // ===== 分页查询 =====
    @Override
    public IPage<Room> searchRoomPage(Integer pageNum, Integer pageSize,
                                      String title, String address,
                                      Integer minPrice, Integer maxPrice,
                                      Integer status, String tag) {
        Page<Room> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), Room::getTitle, title)
                .like(StringUtils.hasText(address), Room::getAddress, address)
                .ge(minPrice != null, Room::getPrice, minPrice)
                .le(maxPrice != null, Room::getPrice, maxPrice)
                .eq(status != null, Room::getStatus, status)
                // 已删除（status=5）的房源在常规列表中隐藏
                .ne(Room::getStatus, 5)
                .like(StringUtils.hasText(tag), Room::getTags, tag)
                .orderByDesc(Room::getCreateTime);
        return baseMapper.selectPage(page, wrapper);
    }

    // ===== 房东房源列表 =====
    @Override
    public List<Room> getMyRooms(Long landlordId) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Room::getLandlordId, landlordId)
                // 已删除（status=5）的房源不再展示给房东
                .ne(Room::getStatus, 5)
                .orderByDesc(Room::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    // ===== 房东更新房源 =====
    @Override
    public boolean updateRoom(Long landlordId, Room room) {
        Room existRoom = this.getById(room.getId());
        if (existRoom == null) {
            throw new RuntimeException("房源不存在");
        }
        if (!existRoom.getLandlordId().equals(landlordId)) {
            throw new RuntimeException("无权编辑他人房源");
        }
        room.setLandlordId(landlordId);
        return this.updateById(room);
    }

    // ===== 基于收藏的个性化推荐 =====
    @Override
    public List<Room> recommendByFavorites(Long userId, int limit) {
        LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getIsDeleted, 0);
        List<Favorite> favorites = favoriteMapper.selectList(favoriteWrapper);

        if (favorites.isEmpty()) {
            return recommendHotRooms(limit);
        }

        List<Long> favoritedRoomIds = favorites.stream()
                .map(Favorite::getRoomId)
                .toList();

        // 获取收藏房源的标签
        List<Room> favoritedRooms = baseMapper.selectBatchIds(favoritedRoomIds);
        List<String> allTags = new ArrayList<>();
        for (Room room : favoritedRooms) {
            if (room.getTags() != null && !room.getTags().isEmpty()) {
                String[] tags = room.getTags().split(",");
                for (String tag : tags) {
                    String trimmed = tag.trim();
                    if (!trimmed.isEmpty()) {
                        allTags.add(trimmed);
                    }
                }
            }
        }

        if (allTags.isEmpty()) {
            return recommendHotRooms(limit);
        }

        // 统计高频标签
        Map<String, Integer> tagCount = new HashMap<>();
        for (String tag : allTags) {
            tagCount.put(tag, tagCount.getOrDefault(tag, 0) + 1);
        }
        List<String> topTags = tagCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // 查询含这些标签的其他房源（排除已收藏）
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getStatus, 1)
                .notIn(Room::getId, favoritedRoomIds);

        if (topTags.size() == 1) {
            roomWrapper.like(Room::getTags, topTags.get(0));
        } else if (topTags.size() == 2) {
            roomWrapper.and(w -> w.like(Room::getTags, topTags.get(0))
                    .or()
                    .like(Room::getTags, topTags.get(1)));
        } else if (topTags.size() >= 3) {
            roomWrapper.and(w -> w.like(Room::getTags, topTags.get(0))
                    .or()
                    .like(Room::getTags, topTags.get(1))
                    .or()
                    .like(Room::getTags, topTags.get(2)));
        }

        roomWrapper.last("LIMIT " + limit);
        return baseMapper.selectList(roomWrapper);
    }

    // ===== 热门房源推荐 =====
    @Override
    public List<Room> recommendHotRooms(int limit) {
        List<Room> hotRooms = baseMapper.selectHotRooms(limit);
        if (hotRooms.isEmpty()) {
            LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Room::getStatus, 1)
                    .orderByDesc(Room::getCreateTime)
                    .last("LIMIT " + limit);
            return baseMapper.selectList(wrapper);
        }
        return hotRooms;
    }
}