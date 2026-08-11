-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: room_rent_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `room_rent_db`
--



--
-- Table structure for table `appointment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `appointment` (
  `id` bigint NOT NULL COMMENT '预约ID（雪花算法生成）',
  `user_id` bigint NOT NULL COMMENT '租客ID',
  `room_id` bigint NOT NULL COMMENT '房源ID',
  `appointment_date` date NOT NULL COMMENT '预约日期',
  `appointment_time` varchar(20) NOT NULL COMMENT '预约时间段（如 上午9:00-10:00）',
  `remark` varchar(200) DEFAULT NULL COMMENT '租客备注',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0-待确认，1-已确认，2-已拒绝，3-已看房',
  `landlord_remark` varchar(200) DEFAULT NULL COMMENT '房东回复备注',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认/拒绝时间',
  `view_time` datetime DEFAULT NULL COMMENT '实际看房时间（status变为3时记录）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约看房表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `banner`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `banner` (
  `id` bigint NOT NULL COMMENT 'ID（雪花算法生成）',
  `title` varchar(100) DEFAULT NULL COMMENT '轮播图标题（描述）',
  `image_url` varchar(500) NOT NULL COMMENT '图片访问地址',
  `link_url` varchar(500) DEFAULT NULL COMMENT '点击跳转链接（如房源详情页/活动页）',
  `sort_order` int DEFAULT '0' COMMENT '排序（数值越大越靠前）',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页轮播图表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_message`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL COMMENT '发送方用户ID',
  `to_user_id` bigint NOT NULL COMMENT '接收方用户ID',
  `content` text,
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '0-未读 1-已读',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_to` (`to_user_id`,`is_read`),
  KEY `idx_from` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dispute`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `dispute` (
  `id` bigint NOT NULL COMMENT '纠纷ID（雪花算法生成）',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `user_id` bigint NOT NULL COMMENT '发起人（租客）ID',
  `room_id` bigint NOT NULL COMMENT '房源ID',
  `reason` varchar(100) NOT NULL COMMENT '纠纷原因（如：押金扣除争议、房屋损坏赔偿、其他）',
  `description` text COMMENT '详细描述',
  `evidence_images` varchar(2000) DEFAULT NULL COMMENT '证据图片（JSON数组）',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0-待受理，1-处理中，2-已解决，3-已驳回',
  `admin_remark` varchar(500) DEFAULT NULL COMMENT '管理员处理备注',
  `resolution` varchar(500) DEFAULT NULL COMMENT '处理结果说明（如：退还押金500元）',
  `handle_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`) COMMENT '一个订单只能发起一次纠纷',
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单纠纷申诉表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `evaluation` (
  `id` bigint NOT NULL COMMENT '评价ID（雪花算法生成）',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '评价人（租客）ID',
  `room_id` bigint NOT NULL COMMENT '房源ID',
  `rating` tinyint(1) NOT NULL COMMENT '评分：1-5星',
  `content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `images` varchar(2000) DEFAULT NULL COMMENT '图片URL列表（JSON数组）',
  `reply_content` varchar(500) DEFAULT NULL COMMENT '房东回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '回复时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`) COMMENT '一个订单只能评价一次',
  KEY `idx_room_id` (`room_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租客评价表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `favorite`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `favorite` (
  `id` bigint NOT NULL COMMENT '收藏ID（雪花算法生成）',
  `user_id` bigint NOT NULL COMMENT '租客用户ID',
  `room_id` bigint NOT NULL COMMENT '房源ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除（0-未删，1-已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_room` (`user_id`,`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房源收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `friend`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '申请方用户ID',
  `friend_id` bigint NOT NULL COMMENT '被申请方用户ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待接受 1-已添加 2-已拒绝',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_friend` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `landlord_application`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `landlord_application` (
  `id` bigint NOT NULL COMMENT '申请ID（雪花算法生成）',
  `user_id` bigint NOT NULL COMMENT '申请人（租客）ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `id_card` varchar(18) NOT NULL COMMENT '身份证号',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `id_card_front` varchar(255) DEFAULT NULL COMMENT '身份证正面照片URL',
  `id_card_back` varchar(255) DEFAULT NULL COMMENT '身份证反面照片URL',
  `business_license` varchar(255) DEFAULT NULL COMMENT '营业执照照片URL（可选）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注/补充说明',
  `status` tinyint(1) DEFAULT '0' COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注（管理员填写）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`) COMMENT '一个用户只能有一条有效申请',
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房东入驻申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `message` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `type` varchar(32) DEFAULT NULL COMMENT '消息类型: share-分享房源, repair-报修反馈, dispute-纠纷消息, landlord-房东消息',
  `content` text COMMENT '消息内容',
  `sender_id` bigint DEFAULT NULL COMMENT '发送者用户ID',
  `user_id` bigint DEFAULT NULL COMMENT '接收者用户ID（消息所属人）',
  `relation_id` bigint DEFAULT NULL COMMENT '关联业务ID：房源ID/报修ID/纠纷ID',
  `is_read` int DEFAULT '0' COMMENT '是否已读：0-未读 1-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notice`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `notice` (
  `id` bigint NOT NULL COMMENT '公告ID（雪花算法生成）',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `type` tinyint DEFAULT '0' COMMENT '类型：0-系统公告，1-活动通知，2-重要通知',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶：0-否，1-是',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0-草稿，1-已发布',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间（仅status=1时有值）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`,`publish_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `repair`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `repair` (
  `id` bigint NOT NULL COMMENT '报修ID（雪花算法生成）',
  `user_id` bigint NOT NULL COMMENT '报修人（租客）ID',
  `room_id` bigint NOT NULL COMMENT '报修房源ID',
  `title` varchar(100) NOT NULL COMMENT '报修标题',
  `description` text COMMENT '报修描述',
  `images` varchar(2000) DEFAULT NULL COMMENT '图片URL列表（JSON数组存储，如["/uploads/repair/xxx.jpg"]）',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0-待处理，1-处理中，2-已完成，3-已关闭',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人（房东/管理员）ID',
  `handler_remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `handle_time` datetime DEFAULT NULL COMMENT '处理完成时间（状态变为2或3时记录）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报修反馈表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `room`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL COMMENT '房源标题（如：靠近考点·安静单间）',
  `description` text COMMENT '房源描述',
  `price` decimal(10,2) NOT NULL COMMENT '月租金（基础价格）',
  `address` varchar(255) NOT NULL COMMENT '详细地址',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度（用于计算距离位置）',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度（用于计算距离位置）',
  `rating` decimal(3,2) DEFAULT '5.00' COMMENT '平台评分（爬取/用户评价动态更新）',
  `tags` varchar(255) DEFAULT NULL COMMENT '房源标签（用逗号隔开，如：安静,考研专区,有空调）',
  `status` tinyint DEFAULT '0' COMMENT '房源状态（0:待租，1:已锁定，2:已出租）',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号（高并发下防止多人同时修改房态）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `landlord_id` bigint DEFAULT NULL COMMENT '所属房东的用户ID',
  `deposit` decimal(10,2) DEFAULT NULL COMMENT '短租押金',
  `cover` varchar(255) DEFAULT NULL COMMENT '房源封面图URL',
  `detail_images` varchar(2000) DEFAULT NULL COMMENT '详情图片URL逗号分隔',
  `admin_remark` varchar(500) DEFAULT NULL COMMENT '管理员备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短期月租房源表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `room_order`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `room_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '唯一订单号 (给用户看的，如：ORD20260716xxx)',
  `user_id` bigint NOT NULL COMMENT '下单人 (用户ID)',
  `room_id` bigint NOT NULL COMMENT '房屋ID (关联房屋表)',
  `room_title_snapshot` varchar(128) NOT NULL COMMENT '房屋名称快照 (防止房东后续改名)',
  `room_cover_snapshot` varchar(255) DEFAULT NULL COMMENT '房屋封面图快照',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `check_in_date` date NOT NULL COMMENT '入住日期',
  `check_out_date` date NOT NULL COMMENT '退租日期',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付, 1-已支付, 2-已取消, 3-已完成',
  `alipay_trade_no` varchar(100) DEFAULT NULL COMMENT '支付宝交易流水号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标识：0-未删除，1-已删除',
  `deposit` decimal(10,2) DEFAULT NULL COMMENT '订单押金快照',
  `admin_remark` varchar(500) DEFAULT NULL COMMENT '管理员备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房屋租赁订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_config`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` bigint NOT NULL COMMENT '配置ID（雪花算法生成）',
  `config_key` varchar(50) NOT NULL COMMENT '配置键（唯一）',
  `config_value` varchar(500) NOT NULL COMMENT '配置值',
  `config_type` varchar(20) DEFAULT 'string' COMMENT '配置类型：string/int/boolean/decimal',
  `description` varchar(200) DEFAULT NULL COMMENT '配置说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_no` varchar(30) NOT NULL COMMENT '系统分配的唯一账号',
  `nickname` varchar(50) NOT NULL COMMENT '用户昵称(可随时修改，可重复)',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `role` tinyint NOT NULL COMMENT '角色类型 (0:管理员, 1:房东, 2:租客)',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像地址',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态：0-待审核 1-审核通过 2-审核驳回',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_no` (`account_no`) COMMENT '保证账号绝对唯一'
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'room_rent_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-11 12:58:23

