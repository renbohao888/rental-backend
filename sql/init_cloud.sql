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

DROP TABLE IF EXISTS `appointment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `appointment` (
  `id` bigint NOT NULL COMMENT '棰勭害ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `user_id` bigint NOT NULL COMMENT '绉熷ID',
  `room_id` bigint NOT NULL COMMENT '鎴挎簮ID',
  `appointment_date` date NOT NULL COMMENT '棰勭害鏃ユ湡',
  `appointment_time` varchar(20) NOT NULL COMMENT '棰勭害鏃堕棿娈碉紙濡?涓婂崍9:00-10:00锛?,
  `remark` varchar(200) DEFAULT NULL COMMENT '绉熷澶囨敞',
  `status` tinyint(1) DEFAULT '0' COMMENT '鐘舵€侊細0-寰呯‘璁わ紝1-宸茬‘璁わ紝2-宸叉嫆缁濓紝3-宸茬湅鎴?,
  `landlord_remark` varchar(200) DEFAULT NULL COMMENT '鎴夸笢鍥炲澶囨敞',
  `confirm_time` datetime DEFAULT NULL COMMENT '纭/鎷掔粷鏃堕棿',
  `view_time` datetime DEFAULT NULL COMMENT '瀹為檯鐪嬫埧鏃堕棿锛坰tatus鍙樹负3鏃惰褰曪級',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鐢宠鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='棰勭害鐪嬫埧琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `banner`
--

DROP TABLE IF EXISTS `banner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banner` (
  `id` bigint NOT NULL COMMENT 'ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `title` varchar(100) DEFAULT NULL COMMENT '杞挱鍥炬爣棰橈紙鎻忚堪锛?,
  `image_url` varchar(500) NOT NULL COMMENT '鍥剧墖璁块棶鍦板潃',
  `link_url` varchar(500) DEFAULT NULL COMMENT '鐐瑰嚮璺宠浆閾炬帴锛堝鎴挎簮璇︽儏椤?娲诲姩椤碉級',
  `sort_order` int DEFAULT '0' COMMENT '鎺掑簭锛堟暟鍊艰秺澶ц秺闈犲墠锛?,
  `status` tinyint(1) DEFAULT '1' COMMENT '鐘舵€侊細0-绂佺敤锛?-鍚敤',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='棣栭〉杞挱鍥捐〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_message`
--

DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL COMMENT '鍙戦€佹柟鐢ㄦ埛ID',
  `to_user_id` bigint NOT NULL COMMENT '鎺ユ敹鏂圭敤鎴稩D',
  `content` text,
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '0-鏈 1-宸茶',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_to` (`to_user_id`,`is_read`),
  KEY `idx_from` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鑱婂ぉ娑堟伅琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dispute`
--

DROP TABLE IF EXISTS `dispute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dispute` (
  `id` bigint NOT NULL COMMENT '绾犵悍ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `order_id` bigint NOT NULL COMMENT '鍏宠仈璁㈠崟ID',
  `user_id` bigint NOT NULL COMMENT '鍙戣捣浜猴紙绉熷锛塈D',
  `room_id` bigint NOT NULL COMMENT '鎴挎簮ID',
  `reason` varchar(100) NOT NULL COMMENT '绾犵悍鍘熷洜锛堝锛氭娂閲戞墸闄や簤璁€佹埧灞嬫崯鍧忚禂鍋裤€佸叾浠栵級',
  `description` text COMMENT '璇︾粏鎻忚堪',
  `evidence_images` varchar(2000) DEFAULT NULL COMMENT '璇佹嵁鍥剧墖锛圝SON鏁扮粍锛?,
  `status` tinyint(1) DEFAULT '0' COMMENT '鐘舵€侊細0-寰呭彈鐞嗭紝1-澶勭悊涓紝2-宸茶В鍐筹紝3-宸查┏鍥?,
  `admin_remark` varchar(500) DEFAULT NULL COMMENT '绠＄悊鍛樺鐞嗗娉?,
  `resolution` varchar(500) DEFAULT NULL COMMENT '澶勭悊缁撴灉璇存槑锛堝锛氶€€杩樻娂閲?00鍏冿級',
  `handle_time` datetime DEFAULT NULL COMMENT '澶勭悊瀹屾垚鏃堕棿',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鎻愪氦鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`) COMMENT '涓€涓鍗曞彧鑳藉彂璧蜂竴娆＄籂绾?,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='璁㈠崟绾犵悍鐢宠瘔琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation`
--

DROP TABLE IF EXISTS `evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation` (
  `id` bigint NOT NULL COMMENT '璇勪环ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `order_id` bigint NOT NULL COMMENT '璁㈠崟ID',
  `user_id` bigint NOT NULL COMMENT '璇勪环浜猴紙绉熷锛塈D',
  `room_id` bigint NOT NULL COMMENT '鎴挎簮ID',
  `rating` tinyint(1) NOT NULL COMMENT '璇勫垎锛?-5鏄?,
  `content` varchar(500) DEFAULT NULL COMMENT '璇勪环鍐呭',
  `images` varchar(2000) DEFAULT NULL COMMENT '鍥剧墖URL鍒楄〃锛圝SON鏁扮粍锛?,
  `reply_content` varchar(500) DEFAULT NULL COMMENT '鎴夸笢鍥炲鍐呭',
  `reply_time` datetime DEFAULT NULL COMMENT '鍥炲鏃堕棿',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '璇勪环鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`) COMMENT '涓€涓鍗曞彧鑳借瘎浠蜂竴娆?,
  KEY `idx_room_id` (`room_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绉熷璇勪环琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `favorite`
--

DROP TABLE IF EXISTS `favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite` (
  `id` bigint NOT NULL COMMENT '鏀惰棌ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `user_id` bigint NOT NULL COMMENT '绉熷鐢ㄦ埛ID',
  `room_id` bigint NOT NULL COMMENT '鎴挎簮ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鏀惰棌鏃堕棿',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '閫昏緫鍒犻櫎锛?-鏈垹锛?-宸插垹锛?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_room` (`user_id`,`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鎴挎簮鏀惰棌琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `friend`
--

DROP TABLE IF EXISTS `friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '鐢宠鏂圭敤鎴稩D',
  `friend_id` bigint NOT NULL COMMENT '琚敵璇锋柟鐢ㄦ埛ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-寰呮帴鍙?1-宸叉坊鍔?2-宸叉嫆缁?,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_friend` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='濂藉弸鍏崇郴琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `landlord_application`
--

DROP TABLE IF EXISTS `landlord_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `landlord_application` (
  `id` bigint NOT NULL COMMENT '鐢宠ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `user_id` bigint NOT NULL COMMENT '鐢宠浜猴紙绉熷锛塈D',
  `real_name` varchar(50) NOT NULL COMMENT '鐪熷疄濮撳悕',
  `id_card` varchar(18) NOT NULL COMMENT '韬唤璇佸彿',
  `phone` varchar(20) NOT NULL COMMENT '鑱旂郴鐢佃瘽',
  `id_card_front` varchar(255) DEFAULT NULL COMMENT '韬唤璇佹闈㈢収鐗嘦RL',
  `id_card_back` varchar(255) DEFAULT NULL COMMENT '韬唤璇佸弽闈㈢収鐗嘦RL',
  `business_license` varchar(255) DEFAULT NULL COMMENT '钀ヤ笟鎵х収鐓х墖URL锛堝彲閫夛級',
  `remark` varchar(500) DEFAULT NULL COMMENT '澶囨敞/琛ュ厖璇存槑',
  `status` tinyint(1) DEFAULT '0' COMMENT '瀹℃牳鐘舵€侊細0-寰呭鏍革紝1-宸查€氳繃锛?-宸叉嫆缁?,
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '瀹℃牳澶囨敞锛堢鐞嗗憳濉啓锛?,
  `audit_time` datetime DEFAULT NULL COMMENT '瀹℃牳鏃堕棿',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鐢宠鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`) COMMENT '涓€涓敤鎴峰彧鑳芥湁涓€鏉℃湁鏁堢敵璇?,
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鎴夸笢鍏ラ┗鐢宠琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` bigint NOT NULL COMMENT '涓婚敭ID',
  `type` varchar(32) DEFAULT NULL COMMENT '娑堟伅绫诲瀷: share-鍒嗕韩鎴挎簮, repair-鎶ヤ慨鍙嶉, dispute-绾犵悍娑堟伅, landlord-鎴夸笢娑堟伅',
  `content` text COMMENT '娑堟伅鍐呭',
  `sender_id` bigint DEFAULT NULL COMMENT '鍙戦€佽€呯敤鎴稩D',
  `user_id` bigint DEFAULT NULL COMMENT '鎺ユ敹鑰呯敤鎴稩D锛堟秷鎭墍灞炰汉锛?,
  `relation_id` bigint DEFAULT NULL COMMENT '鍏宠仈涓氬姟ID锛氭埧婧怚D/鎶ヤ慨ID/绾犵悍ID',
  `is_read` int DEFAULT '0' COMMENT '鏄惁宸茶锛?-鏈 1-宸茶',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` int DEFAULT '0' COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父 1-宸插垹闄?,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绔欏唴娑堟伅琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `id` bigint NOT NULL COMMENT '鍏憡ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `title` varchar(200) NOT NULL COMMENT '鍏憡鏍囬',
  `content` text NOT NULL COMMENT '鍏憡鍐呭',
  `type` tinyint DEFAULT '0' COMMENT '绫诲瀷锛?-绯荤粺鍏憡锛?-娲诲姩閫氱煡锛?-閲嶈閫氱煡',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '鏄惁缃《锛?-鍚︼紝1-鏄?,
  `status` tinyint(1) DEFAULT '0' COMMENT '鐘舵€侊細0-鑽夌锛?-宸插彂甯?,
  `publish_time` datetime DEFAULT NULL COMMENT '鍙戝竷鏃堕棿锛堜粎status=1鏃舵湁鍊硷級',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`,`publish_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='骞冲彴鍏憡琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `repair`
--

DROP TABLE IF EXISTS `repair`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repair` (
  `id` bigint NOT NULL COMMENT '鎶ヤ慨ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `user_id` bigint NOT NULL COMMENT '鎶ヤ慨浜猴紙绉熷锛塈D',
  `room_id` bigint NOT NULL COMMENT '鎶ヤ慨鎴挎簮ID',
  `title` varchar(100) NOT NULL COMMENT '鎶ヤ慨鏍囬',
  `description` text COMMENT '鎶ヤ慨鎻忚堪',
  `images` varchar(2000) DEFAULT NULL COMMENT '鍥剧墖URL鍒楄〃锛圝SON鏁扮粍瀛樺偍锛屽["/uploads/repair/xxx.jpg"]锛?,
  `status` tinyint(1) DEFAULT '0' COMMENT '鐘舵€侊細0-寰呭鐞嗭紝1-澶勭悊涓紝2-宸插畬鎴愶紝3-宸插叧闂?,
  `handler_id` bigint DEFAULT NULL COMMENT '澶勭悊浜猴紙鎴夸笢/绠＄悊鍛橈級ID',
  `handler_remark` varchar(500) DEFAULT NULL COMMENT '澶勭悊澶囨敞',
  `handle_time` datetime DEFAULT NULL COMMENT '澶勭悊瀹屾垚鏃堕棿锛堢姸鎬佸彉涓?鎴?鏃惰褰曪級',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鎻愪氦鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鎶ヤ慨鍙嶉琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `room`
--

DROP TABLE IF EXISTS `room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL COMMENT '鎴挎簮鏍囬锛堝锛氶潬杩戣€冪偣路瀹夐潤鍗曢棿锛?,
  `description` text COMMENT '鎴挎簮鎻忚堪',
  `price` decimal(10,2) NOT NULL COMMENT '鏈堢閲戯紙鍩虹浠锋牸锛?,
  `address` varchar(255) NOT NULL COMMENT '璇︾粏鍦板潃',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '绾害锛堢敤浜庤绠楄窛绂讳綅缃級',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '缁忓害锛堢敤浜庤绠楄窛绂讳綅缃級',
  `rating` decimal(3,2) DEFAULT '5.00' COMMENT '骞冲彴璇勫垎锛堢埇鍙?鐢ㄦ埛璇勪环鍔ㄦ€佹洿鏂帮級',
  `tags` varchar(255) DEFAULT NULL COMMENT '鎴挎簮鏍囩锛堢敤閫楀彿闅斿紑锛屽锛氬畨闈?鑰冪爺涓撳尯,鏈夌┖璋冿級',
  `status` tinyint DEFAULT '0' COMMENT '鎴挎簮鐘舵€侊紙0:寰呯锛?:宸查攣瀹氾紝2:宸插嚭绉燂級',
  `version` int DEFAULT '0' COMMENT '涔愯閿佺増鏈彿锛堥珮骞跺彂涓嬮槻姝㈠浜哄悓鏃朵慨鏀规埧鎬侊級',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `landlord_id` bigint DEFAULT NULL COMMENT '鎵€灞炴埧涓滅殑鐢ㄦ埛ID',
  `deposit` decimal(10,2) DEFAULT NULL COMMENT '鐭鎶奸噾',
  `cover` varchar(255) DEFAULT NULL COMMENT '鎴挎簮灏侀潰鍥綰RL',
  `detail_images` varchar(2000) DEFAULT NULL COMMENT '璇︽儏鍥剧墖URL閫楀彿鍒嗛殧',
  `admin_remark` varchar(500) DEFAULT NULL COMMENT '绠＄悊鍛樺娉?,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐭湡鏈堢鎴挎簮琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `room_order`
--

DROP TABLE IF EXISTS `room_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_order` (
  `id` bigint NOT NULL COMMENT '涓婚敭ID',
  `order_no` varchar(64) NOT NULL COMMENT '鍞竴璁㈠崟鍙?(缁欑敤鎴风湅鐨勶紝濡傦細ORD20260716xxx)',
  `user_id` bigint NOT NULL COMMENT '涓嬪崟浜?(鐢ㄦ埛ID)',
  `room_id` bigint NOT NULL COMMENT '鎴垮眿ID (鍏宠仈鎴垮眿琛?',
  `room_title_snapshot` varchar(128) NOT NULL COMMENT '鎴垮眿鍚嶇О蹇収 (闃叉鎴夸笢鍚庣画鏀瑰悕)',
  `room_cover_snapshot` varchar(255) DEFAULT NULL COMMENT '鎴垮眿灏侀潰鍥惧揩鐓?,
  `total_amount` decimal(10,2) NOT NULL COMMENT '璁㈠崟鎬婚噾棰?,
  `check_in_date` date NOT NULL COMMENT '鍏ヤ綇鏃ユ湡',
  `check_out_date` date NOT NULL COMMENT '閫€绉熸棩鏈?,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '璁㈠崟鐘舵€侊細0-寰呮敮浠? 1-宸叉敮浠? 2-宸插彇娑? 3-宸插畬鎴?,
  `alipay_trade_no` varchar(100) DEFAULT NULL COMMENT '鏀粯瀹濅氦鏄撴祦姘村彿',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '涓嬪崟鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '閫昏緫鍒犻櫎鏍囪瘑锛?-鏈垹闄わ紝1-宸插垹闄?,
  `deposit` decimal(10,2) DEFAULT NULL COMMENT '璁㈠崟鎶奸噾蹇収',
  `admin_remark` varchar(500) DEFAULT NULL COMMENT '绠＄悊鍛樺娉?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鎴垮眿绉熻祦璁㈠崟琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_config`
--

DROP TABLE IF EXISTS `system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL COMMENT '閰嶇疆ID锛堥洩鑺辩畻娉曠敓鎴愶級',
  `config_key` varchar(50) NOT NULL COMMENT '閰嶇疆閿紙鍞竴锛?,
  `config_value` varchar(500) NOT NULL COMMENT '閰嶇疆鍊?,
  `config_type` varchar(20) DEFAULT 'string' COMMENT '閰嶇疆绫诲瀷锛歴tring/int/boolean/decimal',
  `description` varchar(200) DEFAULT NULL COMMENT '閰嶇疆璇存槑',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绯荤粺閰嶇疆琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `account_no` varchar(30) NOT NULL COMMENT '绯荤粺鍒嗛厤鐨勫敮涓€璐﹀彿',
  `nickname` varchar(50) NOT NULL COMMENT '鐢ㄦ埛鏄电О(鍙殢鏃朵慨鏀癸紝鍙噸澶?',
  `password` varchar(100) NOT NULL COMMENT '瀵嗙爜',
  `role` tinyint NOT NULL COMMENT '瑙掕壊绫诲瀷 (0:绠＄悊鍛? 1:鎴夸笢, 2:绉熷)',
  `phone` varchar(20) DEFAULT NULL COMMENT '鎵嬫満鍙?,
  `avatar` varchar(255) DEFAULT NULL COMMENT '澶村儚鍦板潃',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '娉ㄥ唽鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '瀹℃牳鐘舵€侊細0-寰呭鏍?1-瀹℃牳閫氳繃 2-瀹℃牳椹冲洖',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_no` (`account_no`) COMMENT '淇濊瘉璐﹀彿缁濆鍞竴'
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢ㄦ埛琛?;
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
