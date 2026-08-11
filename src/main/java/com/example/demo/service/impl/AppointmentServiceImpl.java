package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.AppointmentVO;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitAppointment(Long userId, Long roomId, LocalDate appointmentDate,
                                     String appointmentTime, String remark) {
        // 1. 校验房源是否存在且已上架
        Room room = roomMapper.selectById(roomId);
        if (room == null || room.getStatus() != 1) {
            throw new RuntimeException("房源不存在或已下架");
        }

        // 2. 校验预约日期不能是过去
        if (appointmentDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("预约日期不能是过去时间");
        }

        // 3. 检查是否已有同一天的待确认预约（防止重复提交）
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getRoomId, roomId)
                .eq(Appointment::getUserId, userId)
                .eq(Appointment::getAppointmentDate, appointmentDate)
                .eq(Appointment::getStatus, 0);
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("您已提交过该日期的看房预约，请等待房东确认");
        }

        // 4. 组装预约数据
        Appointment appointment = new Appointment();
        appointment.setUserId(userId);
        appointment.setRoomId(roomId);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setRemark(remark);
        appointment.setStatus(0); // 待确认

        return this.save(appointment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAppointment(Long appointmentId, Long landlordId, Integer status, String landlordRemark) {
        if (status != 1 && status != 2) {
            throw new RuntimeException("状态参数错误，仅支持1-确认、2-拒绝");
        }

        Appointment appointment = this.getById(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }

        // 校验房源是否属于该房东
        Room room = roomMapper.selectById(appointment.getRoomId());
        if (room == null || !room.getLandlordId().equals(landlordId)) {
            throw new RuntimeException("无权处理他人房源的预约");
        }

        // 校验当前状态是否为待确认
        if (appointment.getStatus() != 0) {
            throw new RuntimeException("该预约已被处理，不可重复操作");
        }

        appointment.setStatus(status);
        appointment.setLandlordRemark(landlordRemark);
        appointment.setConfirmTime(LocalDateTime.now());

        return this.updateById(appointment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsViewed(Long appointmentId, Long userId) {
        Appointment appointment = this.getById(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }

        // 校验是否本人
        if (!appointment.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作他人预约");
        }

        // 只有已确认的预约才能标记已看房
        if (appointment.getStatus() != 1) {
            throw new RuntimeException("只有已确认的预约才能标记已看房");
        }

        appointment.setStatus(3);
        appointment.setViewTime(LocalDateTime.now());

        return this.updateById(appointment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelAppointment(Long appointmentId, Long userId) {
        Appointment appointment = this.getById(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }

        // 校验是否本人
        if (!appointment.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作他人预约");
        }

        // 仅待确认状态的预约可取消；已确认后可联系房东，已看房/已拒绝不可取消
        if (appointment.getStatus() != 0) {
            throw new RuntimeException("该预约已被处理，无法取消");
        }

        return this.removeById(appointmentId);
    }

    @Override
    public IPage<AppointmentVO> getMyAppointments(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<Appointment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getUserId, userId)
                .orderByDesc(Appointment::getCreateTime);
        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        IPage<Appointment> appointmentPage = this.page(page, wrapper);
        return convertToVOPage(appointmentPage);
    }

    @Override
    public IPage<AppointmentVO> getLandlordAppointments(Long landlordId, Integer pageNum, Integer pageSize, Integer status) {
        // 1. 查出该房东的所有房源ID
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getLandlordId, landlordId)
                .select(Room::getId);
        List<Long> roomIds = roomMapper.selectList(roomWrapper)
                .stream()
                .map(Room::getId)
                .toList();

        if (roomIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        // 2. 查询这些房源的预约
        Page<Appointment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Appointment::getRoomId, roomIds)
                .orderByDesc(Appointment::getCreateTime);
        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        IPage<Appointment> appointmentPage = this.page(page, wrapper);
        return convertToVOPage(appointmentPage);
    }

    @Override
    public IPage<AppointmentVO> getAllAppointments(Integer pageNum, Integer pageSize, Integer status) {
        Page<Appointment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Appointment::getCreateTime);
        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        IPage<Appointment> appointmentPage = this.page(page, wrapper);
        return convertToVOPage(appointmentPage);
    }

    @Override
    public AppointmentVO getAppointmentDetail(Long appointmentId) {
        Appointment appointment = this.getById(appointmentId);
        if (appointment == null) {
            return null;
        }
        return convertToVO(appointment);
    }

    @Override
    public boolean deleteAppointment(Long appointmentId) {
        return this.removeById(appointmentId);
    }

    /**
     * 状态数字转文字
     */
    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待确认";
            case 1 -> "已确认";
            case 2 -> "已拒绝";
            case 3 -> "已看房";
            default -> "未知";
        };
    }

    /**
     * 单个转换
     */
    private AppointmentVO convertToVO(Appointment appointment) {
        AppointmentVO vo = new AppointmentVO();
        vo.setId(appointment.getId());
        vo.setRoomId(appointment.getRoomId());
        vo.setUserId(appointment.getUserId());
        vo.setAppointmentDate(appointment.getAppointmentDate());
        vo.setAppointmentTime(appointment.getAppointmentTime());
        vo.setRemark(appointment.getRemark());
        vo.setStatus(appointment.getStatus());
        vo.setStatusText(getStatusText(appointment.getStatus()));
        vo.setLandlordRemark(appointment.getLandlordRemark());
        vo.setConfirmTime(appointment.getConfirmTime());
        vo.setViewTime(appointment.getViewTime());
        vo.setCreateTime(appointment.getCreateTime());

        // 补全房源信息
        Room room = roomMapper.selectById(appointment.getRoomId());
        if (room != null) {
            vo.setRoomTitle(room.getTitle());
            vo.setRoomCover(room.getCover());
            vo.setRoomAddress(room.getAddress());
        }

        // 补全用户信息
        User user = userMapper.selectById(appointment.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserPhone(user.getPhone());
        }

        return vo;
    }

    /**
     * 分页转换
     */
    private IPage<AppointmentVO> convertToVOPage(IPage<Appointment> page) {
        List<Appointment> records = page.getRecords();
        if (records.isEmpty()) {
            Page<AppointmentVO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            return emptyPage;
        }

        // 批量查询
        List<Long> roomIds = records.stream().map(Appointment::getRoomId).distinct().toList();
        List<Long> userIds = records.stream().map(Appointment::getUserId).distinct().toList();

        Map<Long, Room> roomMap = roomMapper.selectBatchIds(roomIds)
                .stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<AppointmentVO> voList = records.stream().map(appointment -> {
            AppointmentVO vo = new AppointmentVO();
            vo.setId(appointment.getId());
            vo.setRoomId(appointment.getRoomId());
            vo.setUserId(appointment.getUserId());
            vo.setAppointmentDate(appointment.getAppointmentDate());
            vo.setAppointmentTime(appointment.getAppointmentTime());
            vo.setRemark(appointment.getRemark());
            vo.setStatus(appointment.getStatus());
            vo.setStatusText(getStatusText(appointment.getStatus()));
            vo.setLandlordRemark(appointment.getLandlordRemark());
            vo.setConfirmTime(appointment.getConfirmTime());
            vo.setViewTime(appointment.getViewTime());
            vo.setCreateTime(appointment.getCreateTime());

            Room room = roomMap.get(appointment.getRoomId());
            if (room != null) {
                vo.setRoomTitle(room.getTitle());
                vo.setRoomCover(room.getCover());
                vo.setRoomAddress(room.getAddress());
            }

            User user = userMap.get(appointment.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
                vo.setUserPhone(user.getPhone());
            }

            return vo;
        }).toList();

        Page<AppointmentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}