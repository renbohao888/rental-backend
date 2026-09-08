package com.anju.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anju.rental.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
