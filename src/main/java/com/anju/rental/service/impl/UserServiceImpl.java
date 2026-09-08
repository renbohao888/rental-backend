package com.anju.rental.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anju.rental.common.JwtUtils;
import com.anju.rental.entity.User;
import com.anju.rental.mapper.UserMapper;
import com.anju.rental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 用来存取短信验证码

    @Override
    public String register(String nickname, String password, Integer role, String phone, String avatar) {
        // 使用雪花算法生成唯一账号
        // 校验手机号是否已被注册
        if (this.count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
            throw new RuntimeException("该手机号已被注册，请直接登录！");
        }
        String accountNo = generateUniqueAccountNo();

        User newUser = new User();
        newUser.setAccountNo(accountNo);
        newUser.setNickname(nickname);
        newUser.setPassword(DigestUtil.bcrypt(password)); // BCrypt 加密
        newUser.setRole(role);
        newUser.setPhone(phone);
        newUser.setAvatar(avatar);

        this.save(newUser);
        return accountNo;
    }

    @Override
    public String login(String accountNo, String password) {

        // 参数校验
        if (accountNo == null || password == null) {
            throw new RuntimeException("账号或密码错误！");
        }

        User user = this.getOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getAccountNo, accountNo));

        // BCrypt 校验
        if (user == null || !DigestUtil.bcryptCheck(password, user.getPassword())) {
            throw new RuntimeException("账号或密码错误！");
        }

        // 账号状态校验（audit_status=1 表示已被管理员禁用）
        if (user.getAuditStatus() != null && user.getAuditStatus() == 1) {
            throw new RuntimeException("该账号已被管理员禁用，请联系平台客服处理！");
        }

        // 登录成功，生成 JWT
        return JwtUtils.generateToken(user.getId(), user.getRole());
    }

    @Override
    public String loginByPhone(String phone, String password) {
        // 参数校验
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 按手机号查询用户（手机号唯一）
        User user = this.getOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone));

        // BCrypt 密码校验
        if (user == null || !DigestUtil.bcryptCheck(password, user.getPassword())) {
            throw new RuntimeException("账号或密码错误！");
        }

        // 账号状态校验（audit_status=1 表示已被管理员禁用）
        if (user.getAuditStatus() != null && user.getAuditStatus() == 1) {
            throw new RuntimeException("该账号已被管理员禁用，请联系平台客服处理！");
        }

        // 登录成功，生成 JWT
        return JwtUtils.generateToken(user.getId(), user.getRole());
    }

    @Override
    public void sendSmsCode(String phone) {
        // 生成一个 6 位随机验证码
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));

        // 将验证码存入 Redis，并设置 5 分钟过期
        String redisKey = "sms:code:" + phone;
        stringRedisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

        // 模拟发送短信
        log.info("【短期租赁平台】发送给手机号 {} 的验证码是：{}，5分钟内有效", phone, code);
    }

    @Override
    public String smsLogin(String phone, String code) {
        // 从 Redis 中拿出刚刚存的验证码
        String redisKey = "sms:code:" + phone;
        String savedCode = stringRedisTemplate.opsForValue().get(redisKey);

        // 校验验证码是否正确或已过期（校验通过后自动作废）
        if (!verifySmsCode(phone, code)) {
            throw new RuntimeException("验证码错误或已过期！");
        }

        // 根据手机号查询用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = this.getOne(queryWrapper);

        // 如果系统里没有这个手机号，说明是新用户
        if (user == null) {
            throw new RuntimeException("该手机号尚未注册，请先使用手机号注册！");
        }

        // 账号状态校验（audit_status=1 表示已被管理员禁用）
        if (user.getAuditStatus() != null && user.getAuditStatus() == 1) {
            throw new RuntimeException("该账号已被管理员禁用，请联系平台客服处理！");
        }

        // 颁发 JWT
        return JwtUtils.generateToken(user.getId(), user.getRole());
    }

    @Override
    public String registerByPhone(String phone, String code, String password, String nickname, Integer role) {
        // 基础校验
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        if (password == null || password.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        if (role != null && role != 1 && role != 2) {
            throw new RuntimeException("角色参数错误，仅支持房东或租客注册");
        }

        // 校验短信验证码
        if (!verifySmsCode(phone, code)) {
            throw new RuntimeException("验证码错误或已过期！");
        }

        // 校验手机号是否已被注册
        if (this.count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
            throw new RuntimeException("该手机号已被注册，请直接登录！");
        }

        // 创建用户（默认租客）
        if (role == null) {
            role = 2;
        }
        String accountNo = generateUniqueAccountNo();
        User newUser = new User();
        newUser.setAccountNo(accountNo);
        newUser.setNickname(nickname == null || nickname.trim().isEmpty() ? "用户" + phone.substring(7) : nickname.trim());
        newUser.setPassword(DigestUtil.bcrypt(password));
        newUser.setRole(role);
        newUser.setPhone(phone);
        this.save(newUser);
        return accountNo;
    }

    @Override
    public boolean resetPassword(String phone, String newPassword) {
        // 基础校验
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }

        // 按手机号查找用户（手机号唯一，校验通过即视为身份确认）
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new RuntimeException("该手机号尚未注册，请先注册");
        }

        // 更新密码
        user.setPassword(DigestUtil.bcrypt(newPassword));
        return this.updateById(user);
    }

    @Override
    public User getPublicUserInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = this.getById(userId);
        if (user == null) {
            return null;
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 校验短信验证码（校验通过后立即作废，防止重复使用）
     */
    private boolean verifySmsCode(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        String redisKey = "sms:code:" + phone;
        String savedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (savedCode == null || !savedCode.equals(code.trim())) {
            return false;
        }
        stringRedisTemplate.delete(redisKey);
        return true;
    }

    /**
     * 生成唯一账号（雪花算法）
     */
    private String generateUniqueAccountNo() {
        return IdUtil.getSnowflake(1, 1).nextIdStr();
    }

    /**
     * 管理员分页查询所有用户
     */
    @Override
    public IPage<User> getAllUsersPage(Integer pageNum, Integer pageSize, Integer role) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return this.page(page, wrapper);
    }
    /**
     * 修改密码（校验旧密码，BCrypt 加密新密码）
     */
    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new RuntimeException("请输入当前密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!DigestUtil.bcryptCheck(oldPassword, user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }
        user.setPassword(DigestUtil.bcrypt(newPassword));
        return this.updateById(user);
    }

}