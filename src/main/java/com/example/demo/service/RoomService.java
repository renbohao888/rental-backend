package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.Room;

import java.util.List;

public interface RoomService extends IService<Room> {
    /**
     * 预定房源核心业务
     * @param roomId 房源ID
     * @param userId 用户ID
     * @return 是否预定成功
     */
    boolean bookRoom(Long roomId, Long userId);

    /**
     * 房源多条件分页查询
     * @param pageNum 当前页
     * @param pageSize 每页条数
     * @param title 房源标题模糊搜索
     * @param address 地址模糊搜索
     * @param minPrice 最低月租
     * @param maxPrice 最高月租
     * @param status 房源状态 0-待审核 1-已上架 2-已租出 3-已下架 4-已驳回
     * @param tag 标签筛选（如"近地铁"）
     * @return 分页结果
     */
    IPage<Room> searchRoomPage(Integer pageNum,
                                   Integer pageSize,
                                   String title,
                                   String address,
                                   Integer minPrice,
                                   Integer maxPrice,
                                   Integer status,
                                   String tag);

    /**
     * 房东查询自己发布的所有房源
     */
    List<Room> getMyRooms(Long landlordId);

    /**
     * 房东编辑房源信息（只能编辑自己的房源）
     */
    boolean updateRoom(Long landlordId, Room room);

    /**
     * 基于用户收藏标签推荐房源
     */
    List<Room> recommendByFavorites(Long userId, int limit);

    /**
     * 热门房源推荐（当用户没有收藏时使用）
     */
    List<Room> recommendHotRooms(int limit);
    }
