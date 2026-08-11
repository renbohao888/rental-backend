package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 站内消息返回视图对象
 */
@Data
public class MessageVO {

    private Long id;

    /** 消息类型：landlord-房东消息 repair-报修反馈 dispute-纠纷消息 share-分享房源 */
    private String type;

    /** 消息内容 */
    private String content;

    private Long senderId;

    /** 发送者昵称 */
    private String senderName;

    /** 发送者头像 */
    private String senderAvatar;

    /** 关联业务ID：如分享时是房源ID，报修时是报修ID，纠纷时是纠纷ID */
    private Long relationId;

    /** 是否已读 */
    private Integer isRead;

    private LocalDateTime createTime;

    /** 房源关联信息（分享消息附带） */
    private RoomInfo roomInfo;

    @Data
    public static class RoomInfo {
        private Long id;
        private String title;
        private String cover;
        private BigDecimal price;
    }
}
