package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.LandlordApplicationVO;

public interface LandlordApplicationService {

    /**
     * 租客提交房东入驻申请
     */
    boolean submitApplication(Long userId, String realName, String idCard, String phone,
                              String remark, String idCardFront, String idCardBack, String businessLicense);

    /**
     * 租客查询自己的申请状态
     */
    LandlordApplicationVO getMyApplication(Long userId);

    /**
     * 管理员分页查询所有申请
     */
    IPage<LandlordApplicationVO> getAllApplications(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员审核申请
     */
    boolean auditApplication(Long applicationId, Integer status, String auditRemark);

    /**
     * 查询申请详情
     */
    LandlordApplicationVO getApplicationDetail(Long applicationId);

    /**
     * 管理员撤销已认证的房东资格（用户降级为租客，名下房源全部下架）
     */
    boolean revokeApplication(Long applicationId, String reason);
}