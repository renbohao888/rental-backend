package com.anju.rental.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.anju.rental.entity.Favorite;
import com.anju.rental.entity.Room;

public interface FavoriteService {

    /**
     * 添加收藏
     */
    boolean addFavorite(Long userId, Long roomId);

    /**
     * 取消收藏（逻辑删除）
     */
    boolean cancelFavorite(Long userId, Long roomId);

    /**
     * 分页查询我的收藏（返回收藏记录，包含房源信息需要联表）
     */
    IPage<Favorite> getMyFavorites(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 分页查询我收藏的房源列表（对齐前端 my/Favorites.vue，返回 Room 对象）
     */
    IPage<Room> getFavoriteRoomPage(Long userId, Integer pageNum, Integer pageSize, String sortBy);

    /**
     * 检查当前用户是否已收藏该房源
     */
    boolean isFavorited(Long userId, Long roomId);
}