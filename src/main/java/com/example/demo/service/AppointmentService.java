package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.AppointmentVO;

import java.time.LocalDate;

public interface AppointmentService {

    /**
     * 租客提交看房预约
     */
    boolean submitAppointment(Long userId, Long roomId, LocalDate appointmentDate,
                              String appointmentTime, String remark);

    /**
     * 房东处理预约（确认/拒绝）
     */
    boolean handleAppointment(Long appointmentId, Long landlordId, Integer status, String landlordRemark);

    /**
     * 租客标记已看房（确认看房完成后）
     */
    boolean markAsViewed(Long appointmentId, Long userId);

    /**
     * 租客取消预约（仅待确认状态可取消，本人操作）
     */
    boolean cancelAppointment(Long appointmentId, Long userId);

    /**
     * 租客查询自己的预约列表
     */
    IPage<AppointmentVO> getMyAppointments(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 房东查询自己房源的预约列表
     */
    IPage<AppointmentVO> getLandlordAppointments(Long landlordId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员查询所有预约列表
     */
    IPage<AppointmentVO> getAllAppointments(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 查询预约详情
     */
    AppointmentVO getAppointmentDetail(Long appointmentId);

    /**
     * 删除预约（管理员）
     */
    boolean deleteAppointment(Long appointmentId);
}