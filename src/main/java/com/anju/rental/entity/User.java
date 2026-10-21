package com.anju.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String accountNo; // 系统账号
    private String nickname;  // 用户昵称

    /** 账号密码（BCrypt 哈希）：禁止随接口响应序列化，防止泄露 */
    @JsonIgnore
    private String password;

    /**
     * 角色：0-管理员, 1-房东, 2-租客
     */
    private Integer role;

    private String phone;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 头像图片外网访问地址 */
    private String avatar;
    /** 用于房东入驻审核 */
    private Integer auditStatus;
}