package com.anju.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anju.rental.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
}