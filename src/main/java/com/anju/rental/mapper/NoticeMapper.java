package com.anju.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anju.rental.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}