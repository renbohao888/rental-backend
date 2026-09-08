package com.anju.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anju.rental.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
