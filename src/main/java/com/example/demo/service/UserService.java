package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.User;

public interface UserService extends IService<User> {

    /**
     * 账号密码注册
     * @param nickname 账号
     * @param password 密码
     * @param role 角色 0管理员/1房东/2租客
     * @param phone 手机号
     * @param avatar 头像地址
     * @return true注册成功
     */
    String register(String nickname, String password, Integer role, String phone, String avatar);

    /**
     * 账号密码登录
     * @param accountNo 账号
     * @param password 密码
     * @return JWT令牌
     */
    String login(String accountNo, String password);

    /**
     * 手机号密码登录
     * @param phone 手机号
     * @param password 密码
     * @return JWT令牌
     */
    String loginByPhone(String phone, String password);

    /**
     * 发送短信验证码
     * @param phone 手机号
     */
    void sendSmsCode(String phone);

    /**
     * 手机号验证码登录
     * @param phone 手机号
     * @param code 短信验证码
     * @return JWT令牌
     */
    String smsLogin(String phone, String code);

    /**
     * 管理员分页查询所有用户
     */
    IPage<User> getAllUsersPage(Integer pageNum, Integer pageSize, Integer role);
    /**
     * 修改密码（校验旧密码，BCrypt 加密新密码）
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 手机号 + 短信验证码注册
     * @param phone 手机号
     * @param code 短信验证码
     * @param password 密码
     * @param nickname 昵称
     * @param role 角色（默认租客 2）
     * @return 生成的账号
     */
    String registerByPhone(String phone, String code, String password, String nickname, Integer role);

    /**
     * 通过手机号找回/重置密码（手机号唯一，确认存在后直接更新密码）
     * @param phone 手机号
     * @param newPassword 新密码
     */
    boolean resetPassword(String phone, String newPassword);

    /**
     * 获取公开用户信息（不含密码，用于房源详情等公开页面展示房东信息）
     * @param userId 用户ID
     */
    User getPublicUserInfo(Long userId);

}