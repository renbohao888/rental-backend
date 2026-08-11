package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.Room;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
    @Select("SELECT * FROM favorite WHERE user_id = #{userId} AND room_id = #{roomId}")
    Favorite selectByUserAndRoom(@Param("userId") Long userId, @Param("roomId") Long roomId);

    @Delete("DELETE FROM favorite WHERE user_id = #{userId} AND room_id = #{roomId}")
    int physicalDeleteByUserIdAndRoomId(@Param("userId") Long userId,
                                        @Param("roomId") Long roomId);

    /**
     * 查询用户收藏的房源（去重、按收藏时间倒序）
     */
    @Select("SELECT r.* FROM favorite f " +
            "INNER JOIN room r ON f.room_id = r.id " +
            "WHERE f.user_id = #{userId} AND f.is_deleted = 0 " +
            "ORDER BY f.create_time DESC")
    List<Room> selectFavoriteRooms(@Param("userId") Long userId);
}