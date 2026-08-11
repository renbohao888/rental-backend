package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoomMapper extends BaseMapper<Room> {
    /**
     * 查询热门房源（按已完成订单数排序）
     */
    @Select("SELECT r.* FROM room r " +
            "INNER JOIN room_order o ON r.id = o.room_id " +
            "WHERE r.status = 1 AND o.status = 4 " +
            "GROUP BY r.id " +
            "ORDER BY COUNT(o.id) DESC " +
            "LIMIT #{limit}")
    List<Room> selectHotRooms(@Param("limit") int limit);
    // 继承了 BaseMapper，你就自动拥有了 insert, delete, update, selectList 等所有方法
}
