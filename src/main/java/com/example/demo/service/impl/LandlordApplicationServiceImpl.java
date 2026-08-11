package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.LandlordApplicationVO;
import com.example.demo.entity.LandlordApplication;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import com.example.demo.mapper.LandlordApplicationMapper;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.LandlordApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LandlordApplicationServiceImpl
        extends ServiceImpl<LandlordApplicationMapper, LandlordApplication>
        implements LandlordApplicationService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitApplication(Long userId, String realName, String idCard, String phone,
                                     String remark, String idCardFront, String idCardBack, String businessLicense) {
        // 1. 检查是否已有申请
        LambdaQueryWrapper<LandlordApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LandlordApplication::getUserId, userId);
        LandlordApplication exist = this.getOne(wrapper);
        if (exist != null && exist.getStatus() == 0) {
            throw new RuntimeException("您已提交过入驻申请，请等待审核");
        }
        if (exist != null && exist.getStatus() == 1) {
            throw new RuntimeException("您已是房东，无需重复申请");
        }

        // 2. 检查用户是否存在且为租客
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getRole() != 2) {
            throw new RuntimeException("只有租客才能申请成为房东");
        }

        // 3. 组装申请数据（若之前被驳回/撤销，复用原记录重新提交，避免同一用户产生多条记录）
        LandlordApplication application;
        if (exist != null && (exist.getStatus() == 2 || exist.getStatus() == 3)) {
            application = exist;
        } else {
            application = new LandlordApplication();
            application.setUserId(userId);
        }
        application.setRealName(realName);
        application.setIdCard(idCard);
        application.setPhone(phone);
        application.setRemark(remark);
        application.setIdCardFront(idCardFront);
        application.setIdCardBack(idCardBack);
        application.setBusinessLicense(businessLicense);
        application.setStatus(0); // 待审核
        application.setAuditRemark(null);
        application.setAuditTime(null);

        // 复用已存在记录时，用 UpdateWrapper 显式更新（含清空审核备注/审核时间）
        if (application.getId() != null) {
            LambdaUpdateWrapper<LandlordApplication> uw = new LambdaUpdateWrapper<>();
            uw.eq(LandlordApplication::getId, application.getId())
                    .set(LandlordApplication::getRealName, realName)
                    .set(LandlordApplication::getIdCard, idCard)
                    .set(LandlordApplication::getPhone, phone)
                    .set(LandlordApplication::getRemark, remark)
                    .set(LandlordApplication::getIdCardFront, idCardFront)
                    .set(LandlordApplication::getIdCardBack, idCardBack)
                    .set(LandlordApplication::getBusinessLicense, businessLicense)
                    .set(LandlordApplication::getStatus, 0)
                    .set(LandlordApplication::getAuditRemark, null)
                    .set(LandlordApplication::getAuditTime, null);
            return this.update(null, uw);
        }
        return this.save(application);
    }

    @Override
    public LandlordApplicationVO getMyApplication(Long userId) {
        LambdaQueryWrapper<LandlordApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LandlordApplication::getUserId, userId)
                .orderByDesc(LandlordApplication::getCreateTime)
                .last("LIMIT 1");
        LandlordApplication application = this.getOne(wrapper, false);
        if (application == null) {
            return null;
        }
        return convertToVO(application);
    }

    @Override
    public IPage<LandlordApplicationVO> getAllApplications(Integer pageNum, Integer pageSize, Integer status) {
        Page<LandlordApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LandlordApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LandlordApplication::getCreateTime);
        if (status != null) {
            wrapper.eq(LandlordApplication::getStatus, status);
        }
        IPage<LandlordApplication> applicationPage = this.page(page, wrapper);
        return convertToVOPage(applicationPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditApplication(Long applicationId, Integer status, String auditRemark) {
        if (status != 1 && status != 2) {
            throw new RuntimeException("审核状态参数错误，仅支持1-通过、2-拒绝");
        }

        LandlordApplication application = this.getById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (application.getStatus() != 0) {
            throw new RuntimeException("该申请已被处理，不可重复操作");
        }

        // 更新申请状态
        application.setStatus(status);
        application.setAuditRemark(auditRemark);
        application.setAuditTime(LocalDateTime.now());
        this.updateById(application);

        // 如果审核通过，升级用户角色为房东
        if (status == 1) {
            User user = userMapper.selectById(application.getUserId());
            if (user != null && user.getRole() == 2) {
                user.setRole(1);
                userMapper.updateById(user);
            }
        }

        return true;
    }

    @Override
    public LandlordApplicationVO getApplicationDetail(Long applicationId) {
        LandlordApplication application = this.getById(applicationId);
        if (application == null) {
            return null;
        }
        return convertToVO(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeApplication(Long applicationId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("撤销原因不能为空");
        }

        LandlordApplication application = this.getById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请记录不存在");
        }
        if (application.getStatus() != 1) {
            throw new RuntimeException("仅已认证通过的房东可被撤销");
        }

        Long userId = application.getUserId();

        // 1. 申请记录状态改为已撤销（3），记录撤销原因
        LambdaUpdateWrapper<LandlordApplication> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LandlordApplication::getId, applicationId)
                .set(LandlordApplication::getStatus, 3)
                .set(LandlordApplication::getAuditRemark, reason.trim())
                .set(LandlordApplication::getAuditTime, LocalDateTime.now());
        this.update(null, wrapper);

        // 2. 用户角色降级为租客
        User user = userMapper.selectById(userId);
        if (user != null && user.getRole() == 1) {
            user.setRole(2);
            userMapper.updateById(user);
        }

        // 3. 名下所有房源强制下架（含待审核、已上架、已租出）
        LambdaUpdateWrapper<Room> roomWrapper = new LambdaUpdateWrapper<>();
        roomWrapper.eq(Room::getLandlordId, userId)
                .in(Room::getStatus, 0, 1, 2)
                .set(Room::getStatus, 3)
                .set(Room::getAdminRemark, "房东资质被撤销：" + reason.trim());
        roomMapper.update(null, roomWrapper);

        return true;
    }

    /**
     * 状态数字转文字
     */
    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            case 3 -> "已撤销";
            default -> "未知";
        };
    }

    /**
     * 单个转换
     */
    private LandlordApplicationVO convertToVO(LandlordApplication application) {
        LandlordApplicationVO vo = new LandlordApplicationVO();
        vo.setId(application.getId());
        vo.setUserId(application.getUserId());
        vo.setRealName(application.getRealName());
        vo.setIdCard(application.getIdCard());
        vo.setPhone(application.getPhone());
        vo.setIdCardFront(application.getIdCardFront());
        vo.setIdCardBack(application.getIdCardBack());
        vo.setBusinessLicense(application.getBusinessLicense());
        vo.setRemark(application.getRemark());
        vo.setStatus(application.getStatus());
        vo.setStatusText(getStatusText(application.getStatus()));
        vo.setAuditRemark(application.getAuditRemark());
        vo.setAuditTime(application.getAuditTime());
        vo.setCreateTime(application.getCreateTime());

        // 补全用户信息
        User user = userMapper.selectById(application.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserPhone(user.getPhone());
        }

        return vo;
    }

    /**
     * 分页转换
     */
    private IPage<LandlordApplicationVO> convertToVOPage(IPage<LandlordApplication> page) {
        List<LandlordApplication> records = page.getRecords();
        if (records.isEmpty()) {
            Page<LandlordApplicationVO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            return emptyPage;
        }

        List<Long> userIds = records.stream().map(LandlordApplication::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<LandlordApplicationVO> voList = records.stream().map(app -> {
            LandlordApplicationVO vo = convertToVO(app);
            User user = userMap.get(app.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
                vo.setUserPhone(user.getPhone());
            }
            return vo;
        }).toList();

        Page<LandlordApplicationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}