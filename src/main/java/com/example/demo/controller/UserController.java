package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.FileUploadUtils;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileUploadUtils fileUploadUtils;  // 👈 注入文件上传工具

    /**
     * 账号密码注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> params) {
        Map<String, Object> response = new HashMap<>();

        try {
            String nickname = params.get("nickname");
            String password = params.get("password");
            String roleStr = params.get("role");
            String phone = params.get("phone");
            String avatar = params.get("avatar");

            if (nickname == null || nickname.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "昵称不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "密码不能为空");
            }
            if (password.length() < 6) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "密码长度不能少于6位");
            }
            if (roleStr == null || roleStr.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "角色不能为空");
            }
            if (phone == null || phone.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号不能为空");
            }
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号格式不正确");
            }

            Integer role;
            try {
                role = Integer.valueOf(roleStr);
            } catch (NumberFormatException e) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "角色参数格式错误");
            }

            // 注册页上传的头像是 base64（data URL），转换为服务器图片地址后存储
            if (avatar != null && avatar.startsWith("data:image/")) {
                avatar = fileUploadUtils.saveBase64Image(avatar, "avatar");
            }

            String accountNo = userService.register(nickname, password, role, phone, avatar);

            response.put("code", 200);
            response.put("message", "注册成功");
            Map<String, String> data = new HashMap<>();
            data.put("accountNo", accountNo);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "注册失败，请稍后再试");
        }
    }

    /**
     * 账号密码登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        Map<String, Object> response = new HashMap<>();

        try {
            String accountNo = params.get("accountNo");
            String password = params.get("password");

            if (accountNo == null || accountNo.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "账号不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "密码不能为空");
            }

            String token = userService.login(accountNo, password);

            response.put("code", 200);
            response.put("message", "登录成功");
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "登录失败，请稍后再试");
        }
    }

    /**
     * 手机号密码登录
     */
    @PostMapping("/loginByPhone")
    public ResponseEntity<Map<String, Object>> loginByPhone(@RequestBody Map<String, String> params) {
        Map<String, Object> response = new HashMap<>();

        try {
            String phone = params.get("phone");
            String password = params.get("password");

            if (phone == null || phone.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "密码不能为空");
            }

            String token = userService.loginByPhone(phone, password);

            response.put("code", 200);
            response.put("message", "登录成功");
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "登录失败，请稍后再试");
        }
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/sendSms")
    public ResponseEntity<Map<String, Object>> sendSms(@RequestParam(required = false) String phone) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (phone == null || phone.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号不能为空");
            }
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号格式不正确");
            }

            userService.sendSmsCode(phone);

            response.put("code", 200);
            response.put("message", "短信发送成功");
            response.put("data", null);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "发送失败，请稍后再试");
        }
    }

    /**
     * 手机验证码登录
     */
    @PostMapping("/smsLogin")
    public ResponseEntity<Map<String, Object>> smsLogin(@RequestBody Map<String, String> params) {
        Map<String, Object> response = new HashMap<>();

        try {
            String phone = params.get("phone");
            String code = params.get("code");

            if (phone == null || phone.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号不能为空");
            }
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号格式不正确");
            }
            if (code == null || code.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "验证码不能为空");
            }

            String token = userService.smsLogin(phone, code);

            response.put("code", 200);
            response.put("message", "登录成功");
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "登录失败，请稍后再试");
        }
    }

    /**
     * 手机号 + 短信验证码注册
     */
    @PostMapping("/registerByPhone")
    public ResponseEntity<Map<String, Object>> registerByPhone(@RequestBody Map<String, String> params) {
        Map<String, Object> response = new HashMap<>();

        try {
            String phone = params.get("phone");
            String code = params.get("code");
            String password = params.get("password");
            String nickname = params.get("nickname");
            String roleStr = params.get("role");

            if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号格式不正确");
            }
            if (code == null || code.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "验证码不能为空");
            }
            if (password == null || password.length() < 6) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "密码长度不能少于6位");
            }

            Integer role = null;
            if (roleStr != null && !roleStr.trim().isEmpty()) {
                try {
                    role = Integer.valueOf(roleStr);
                } catch (NumberFormatException e) {
                    return buildErrorResponse(HttpStatus.BAD_REQUEST, "角色参数格式错误");
                }
            }

            String accountNo = userService.registerByPhone(phone, code, password, nickname, role);

            response.put("code", 200);
            response.put("message", "注册成功");
            Map<String, String> data = new HashMap<>();
            data.put("accountNo", accountNo);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "注册失败，请稍后再试");
        }
    }

    /**
     * 找回密码（手机号唯一，校验存在后直接更新密码）
     */
    @PostMapping("/resetPassword")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> params) {
        Map<String, Object> response = new HashMap<>();

        try {
            String phone = params.get("phone");
            String newPassword = params.get("newPassword");

            if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "手机号格式不正确");
            }
            if (newPassword == null || newPassword.length() < 6) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "新密码长度不能少于6位");
            }

            boolean success = userService.resetPassword(phone, newPassword);
            if (!success) {
                return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "重置密码失败，请稍后再试");
            }

            response.put("code", 200);
            response.put("message", "密码重置成功，请使用新密码登录");
            response.put("data", null);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "重置密码失败，请稍后再试");
        }
    }

    /**
     * 公开用户信息（用于房源详情页展示房东信息，不返回密码）
     */
    @GetMapping("/detail/{userId}")
    public Result<User> getUserDetail(@PathVariable Long userId) {
        User user = userService.getPublicUserInfo(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(user);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", status.value());
        response.put("message", message);
        response.put("data", null);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("realUserId");
            if (userId == null) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "请先登录");
            }
            User user = userService.getById(userId);
            if (user == null) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "用户不存在");
            }
            user.setPassword(null);

            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "获取用户信息失败");
        }
    }

    /**
     * 管理员分页查询所有用户
     */
    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public IPage<User> getAllUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer role) {
        return userService.getAllUsersPage(pageNum, pageSize, role);
    }

    /**
     * 管理员禁用/启用用户账号（audit_status：0-正常 1-禁用）
     */
    @PostMapping("/admin/toggle-status/{userId}")
    @RequiresRoles({0})
    public Result<String> toggleUserStatus(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestParam Integer status) {
        if (status != 0 && status != 1) {
            return Result.fail("状态参数错误，仅支持 0-启用 1-禁用");
        }
        Long adminId = (Long) request.getAttribute("realUserId");
        if (userId.equals(adminId)) {
            return Result.fail("不能禁用当前登录的管理员账号");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (status == 1 && user.getRole() == 0) {
            return Result.fail("不能禁用管理员账号");
        }
        user.setAuditStatus(status);
        boolean success = userService.updateById(user);
        return success ? Result.success(status == 1 ? "账号已禁用" : "账号已启用") : Result.fail("操作失败");
    }

    /**
     * 更新用户个人信息（昵称、头像等）
     */
    @PutMapping("/update")
    @RequiresRoles({0, 1, 2})
    public Result<String> updateUserInfo(HttpServletRequest request, @RequestBody User user) {
        Long userId = (Long) request.getAttribute("realUserId");
        User existUser = userService.getById(userId);
        if (existUser == null) {
            return Result.fail("用户不存在");
        }
        if (user.getNickname() != null) {
            existUser.setNickname(user.getNickname());
        }
        if (user.getAvatar() != null) {
            existUser.setAvatar(user.getAvatar());
        }
        boolean success = userService.updateById(existUser);
        return success ? Result.success("更新成功") : Result.fail("更新失败");
    }
    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    @RequiresRoles({0, 1, 2})
    public Result<String> changePassword(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("realUserId");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        try {
            boolean success = userService.changePassword(userId, oldPassword, newPassword);
            return success ? Result.success("密码修改成功") : Result.fail("修改失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }


    /**
     * 头像上传接口
     */
    @PostMapping("/uploadAvatar")
    @RequiresRoles({0, 1, 2})
    public Result<String> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Long userId = (Long) request.getAttribute("realUserId");
        try {
            List<String> urls = fileUploadUtils.uploadImages(List.of(file), "avatar");
            if (urls.isEmpty()) {
                return Result.fail("上传失败");
            }
            String avatarUrl = urls.get(0);
            User user = userService.getById(userId);
            user.setAvatar(avatarUrl);
            userService.updateById(user);
            return Result.success(avatarUrl);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}