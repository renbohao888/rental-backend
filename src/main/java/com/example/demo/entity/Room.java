package com.example.demo.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO) // 声明主键自增
    private Long id;
    private String title;
    /** 房源描述 */
    private String description;
    private BigDecimal price;
    private String address;
    @TableField(exist = false)
    private String alipayTradeNo;
    private Long landlordId;
    /** 房源封面图片地址 */
    private String cover;
    /** 详情图片URL列表，逗号分隔 */
    private String detailImages;
    // 推荐算法因子
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal rating;
    private String tags;
    //短租押金
    private BigDecimal deposit;
    // 房源状态：0-待审核，1-已上架，2-已租出，3-已下架，4-已驳回
    private Integer status;

    // 版本号字段（已移除乐观锁注解，避免 updateById 触发 MP 乐观锁绑定异常导致支付/审核失败）
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /**
     * 管理员备注（用于强制下架等操作记录）
     */
    private String adminRemark;
}
