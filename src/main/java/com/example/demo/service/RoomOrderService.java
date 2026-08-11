package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.BusinessStatsVO;
import com.example.demo.dto.DashboardVO;
import com.example.demo.dto.RoomBillSummaryVO;
import com.example.demo.entity.RoomOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RoomOrderService extends IService<RoomOrder> {

    String generateOrderNo();

    /**
     * 创建订单 (核心业务方法)
     */
    RoomOrder createOrder(Long userId, Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * 查询某个用户的所有订单
     */
    List<RoomOrder> getUserOrders(Long userId);

    /**
     * 分页查询某个用户的订单（支持状态过滤）——租客端我的订单
     */
    IPage<RoomOrder> getUserOrdersPage(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 删除单个订单（软删除）
     */
    boolean deleteOrder(Long orderId, Long userId);

    /**
     * 批量删除订单（软删除）
     */
    int batchDeleteOrders(List<Long> orderIds, Long userId);

    /**
     * 房东删除自己房源下的订单（软删除）
     */
    boolean deleteLandlordOrder(Long orderId, Long landlordId);

    /**
     * 房东批量删除自己房源下的订单（软删除）
     */
    int batchDeleteLandlordOrders(List<Long> orderIds, Long landlordId);

    /**
     * 物理删除订单（管理员功能）
     */
    boolean forceDeleteOrder(Long orderId);

    /**
     * 确认入住（已支付待入住 → 已入住）
     */
    boolean checkIn(Long orderId, Long currentUserId, Integer role);

    /**
     * 租客申请退房
     */
    boolean applyCheckOut(Long orderId, Long userId);

    /**
     * 管理员/房东确认退房、完结订单
     */
    boolean completeOrder(Long orderId, Long currentUserId, Integer role, BigDecimal deductAmount);

    /**
     * 租客撤销订单（房东处理前可撤销）
     */
    boolean cancelOrder(Long orderId, Long userId);

    /**
     * 房东拒单（不受理租房申请）
     */
    boolean rejectOrder(Long orderId, Long landlordId);

    /**
     * 管理员分页查询所有订单
     */
    IPage<RoomOrder> getAllOrdersPage(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 房东查询自己房源的所有订单
     */
    List<RoomOrder> getLandlordOrders(Long landlordId);

    /**
     * 房东查看自己名下房源的账单汇总（按房源维度）
     */
    List<RoomBillSummaryVO> getLandlordBillSummary(Long landlordId);


    /**
     * 获取房东的经营统计数据
     */
    BusinessStatsVO getBusinessStats(Long landlordId);


    /**
     * 管理员获取平台运营数据大屏
     */
    DashboardVO getDashboardStats();

    /**
     * 管理员强制退款（取消订单并退还全部金额）
     * 适用状态：待支付(0)、已支付待入住(1)、已入住(2)、退租中(3)
     */
    boolean adminForceRefund(Long orderId, Long adminId, String reason);

    /**
     * 管理员强制退房（完结订单并释放房源，不扣除押金）
     * 适用状态：已入住(2)、退租中(3)
     */
    boolean adminForceCheckout(Long orderId, Long adminId, String reason);

    /**
     * 管理员强制撤销房源（下架房源并取消该房源所有待处理订单）
     */
    boolean adminForceRemoveRoom(Long roomId, Long adminId, String reason);
}