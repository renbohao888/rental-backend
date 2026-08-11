package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.RoomOrder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomOrderMapper extends BaseMapper<RoomOrder> {
    // 基础的增删改查已经由 BaseMapper 提供
    /**
     * 管理员物理删除订单（绕过 @TableLogic 逻辑删除，真正从数据库删除记录）
     */
    @Delete("DELETE FROM room_order WHERE id = #{id}")
    int physicallyDeleteById(Long id);
}