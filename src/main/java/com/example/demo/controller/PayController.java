package com.example.demo.controller;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomOrder;
import com.example.demo.common.Result;
import com.example.demo.service.RoomOrderService;
import com.example.demo.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest; //  新版 Spring Boot 3.x 的写法
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pay")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class PayController {

    @Value("${alipay.appId}")
    private String appId;
    @Value("${alipay.appPrivateKey}")
    private String privateKey;
    @Value("${alipay.alipayPublicKey}")
    private String publicKey;
    @Value("${alipay.serverUrl}")
    private String serverUrl;
    @Value("${alipay.domain}")
    private String domain; // 你的外网穿透地址

    @Autowired
    private RoomOrderService roomOrderService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 模拟支付成功（跳过支付宝沙箱，直接标记订单为已支付）
     * 租客 / 房东均可支付自己的订单
     */
    @PostMapping("/simulate")
    @RequiresRoles({1, 2})
    @Transactional(rollbackFor = Exception.class)
    public Result<String> simulatePay(HttpServletRequest request, @RequestParam String orderNo) {
        LambdaQueryWrapper<RoomOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoomOrder::getOrderNo, orderNo);
        RoomOrder order = roomOrderService.getOne(queryWrapper);

        if (order == null) {
            return Result.fail("订单不存在");
        }
        // 归属校验：只能支付自己名下的订单，防止他人代付/误付
        Long userId = (Long) request.getAttribute("realUserId");
        if (userId == null || !order.getUserId().equals(userId)) {
            return Result.fail("无权支付他人的订单");
        }
        if (order.getStatus() != 0) {
            return Result.fail("订单状态异常，无法支付");
        }

        order.setStatus(1);
        order.setAlipayTradeNo("SIMULATED_" + System.currentTimeMillis());
        roomOrderService.updateById(order);

        // 锁定房源
        Room room = roomService.getById(order.getRoomId());
        if (room != null && room.getStatus() == 1) {
            room.setStatus(2);
            roomService.updateById(room);
        }

        return Result.success("支付成功（模拟）");
    }

    /**
     * 1. 发起支付
     */
    @GetMapping(value = "/aliPay", produces = "text/html;charset=UTF-8")
    public String pay(@RequestParam String orderNo, jakarta.servlet.http.HttpServletRequest httpRequest) throws Exception {
        // 根据唯一订单号查询你的 RoomOrder
        LambdaQueryWrapper<RoomOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoomOrder::getOrderNo, orderNo);
        RoomOrder order = roomOrderService.getOne(queryWrapper);

        if (order == null || order.getStatus() != 0) {
            return "订单不存在或已经支付过了！";
        }

        AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, appId, privateKey, "json", "UTF-8", publicKey, "RSA2");
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        // 支持自定义支付完成后的回跳地址（默认回到前端首页）
        String redirect = httpRequest.getParameter("redirect");
        if (redirect == null || redirect.trim().isEmpty()) {
            redirect = "http://localhost:5173/";
        }
        request.setNotifyUrl(domain + "/api/pay/notify"); // 异步回调地址
        request.setReturnUrl(redirect);                  // 支付完跳回前端哪个页面

        // 使用你表里的真实数据发起支付！
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo()); // 使用你的 ORD2026xxxx 订单号
        bizContent.put("total_amount", order.getTotalAmount().toString()); // 你的总金额
        bizContent.put("subject", "预定房源：" + order.getRoomTitleSnapshot()); // 你的房屋快照名
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        request.setBizContent(objectMapper.writeValueAsString(bizContent));
        return alipayClient.pageExecute(request).getBody();
    }

    /**
     * 2. 支付宝支付成功后的异步回调
     */
    @PostMapping("/notify")
    @Transactional(rollbackFor = Exception.class)
    public String notify(HttpServletRequest request) {
        String outTradeNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");
        String tradeStatus = request.getParameter("trade_status");

        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            // 查出这笔订单，把状态从 0(待支付) 改为 1(已支付待入住)，并存入流水号
            LambdaQueryWrapper<RoomOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RoomOrder::getOrderNo, outTradeNo);
            RoomOrder order = roomOrderService.getOne(wrapper);

            if (order != null && order.getStatus() == 0) {
                order.setStatus(1);
                order.setAlipayTradeNo(tradeNo);
                roomOrderService.updateById(order);

                // 支付成功后，锁定房源状态为已租（防止被其他人重复预订）
                Room room = roomService.getById(order.getRoomId());
                if (room != null && room.getStatus() == 1) {
                    room.setStatus(2);
                    roomService.updateById(room);
                }

                System.out.println("🎉 订单支付成功！订单号: " + outTradeNo + "，房源已锁定");
            }
        }
        return "success";
    }
}