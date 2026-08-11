package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Room;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 租赁助手 AI 实现（本地检索式智能助手）
 * <p>
 * 说明：该 AI 直接读取平台房源数据库，通过意图解析（价格区间 / 区域 /
 * 户型 / 关键词）筛选出符合用户需求的房源并生成推荐回答，不依赖任何外部大模型。
 */
@Service
public class AiServiceImpl implements AiService {

    /** 常见城市区名（用于识别用户提到的区域） */
    private static final List<String> DISTRICTS = Arrays.asList(
            // 广州
            "天河", "海珠", "越秀", "荔湾", "白云", "番禺", "黄埔", "花都", "增城", "从化", "南沙",
            // 深圳
            "南山", "福田", "罗湖", "宝安", "龙岗", "龙华", "盐田",
            // 北京
            "朝阳", "海淀", "丰台", "通州", "昌平",
            // 上海
            "浦东", "徐汇", "静安", "黄浦", "闵行", "长宁",
            // 杭州
            "西湖", "拱墅", "滨江", "上城", "余杭"
    );

    /** 房源关键词（地铁 / 精装 等） */
    private static final List<String> KEYWORDS = Arrays.asList(
            "地铁", "精装", "南向", "电梯", "拎包入住", "家具", "家电", "可短租", "独卫", "近商圈"
    );

    @Autowired
    private RoomMapper roomMapper;

    /** 内部意图结构 */
    private static class Intent {
        Integer minPrice;
        Integer maxPrice;
        String address;
        String layout;
        String keyword;
    }

    @Override
    public Map<String, Object> chat(String message) {
        String q = message == null ? "" : message.trim();
        Map<String, Object> result = new HashMap<>();

        // 1. 问候 / 寒暄
        if (isGreeting(q)) {
            result.put("reply", buildGreeting());
            result.put("rooms", topRooms(3).stream().map(this::toMap).collect(Collectors.toList()));
            return result;
        }

        // 2. 意图解析
        Intent intent = parseIntent(q);

        // 3. 检索房源（无任何条件时返回热门）
        List<Room> rooms = searchRooms(intent);

        // 4. 生成回答
        result.put("reply", buildReplyText(intent, rooms));
        result.put("rooms", rooms.stream().map(this::toMap).collect(Collectors.toList()));
        return result;
    }

    // ===================== 意图解析 =====================

    private boolean isGreeting(String q) {
        if (q.length() > 12) {
            return false;
        }
        return q.matches(".*(你好|您好|嗨|哈喽|hello|hi|hey|在吗|有人吗|早上好|晚上好|下午好).*");
    }

    private Intent parseIntent(String q) {
        Intent intent = new Intent();

        // ---- 价格区间：如 "1000到1500" "800-1200元" ----
        Matcher range = Pattern.compile("(\\d+)\\s*(?:[到至~]|-[\\s]*)\\s*(\\d+)\\s*(?:元|块)?").matcher(q);
        if (range.find()) {
            intent.minPrice = Integer.parseInt(range.group(1));
            intent.maxPrice = Integer.parseInt(range.group(2));
            if (intent.minPrice > intent.maxPrice) {
                int tmp = intent.minPrice;
                intent.minPrice = intent.maxPrice;
                intent.maxPrice = tmp;
            }
        }

        // ---- 价格上限：如 "不超过1500" "1500以内" "预算2000" ----
        if (intent.maxPrice == null) {
            Matcher max1 = Pattern.compile("(?:不超过|低于|小于|最多|预算|封顶|控制在)\\s*(\\d+)\\s*(?:元|块)?").matcher(q);
            if (max1.find()) {
                intent.maxPrice = Integer.parseInt(max1.group(1));
            } else {
                Matcher max2 = Pattern.compile("(\\d+)\\s*(?:元|块)?\\s*(?:以内|以下|封顶)").matcher(q);
                if (max2.find()) {
                    intent.maxPrice = Integer.parseInt(max2.group(1));
                }
            }
        }

        // ---- 价格下限：如 "1500以上" "2000元起" ----
        if (intent.minPrice == null) {
            Matcher min = Pattern.compile("(\\d+)\\s*(?:元|块)?\\s*(?:以上|起|起步|往上)").matcher(q);
            if (min.find()) {
                intent.minPrice = Integer.parseInt(min.group(1));
            }
        }

        // ---- 区域 ----
        for (String d : DISTRICTS) {
            if (q.contains(d)) {
                intent.address = d;
                break;
            }
        }

        // ---- 户型 ----
        if (q.matches(".*(单间|一室|一居|一房|1室|公寓).*")) {
            intent.layout = "一室";
        } else if (q.matches(".*(两室|两居|两房|二室|二居|2室).*")) {
            intent.layout = "两室";
        } else if (q.matches(".*(三室|三居|三房|3室).*")) {
            intent.layout = "三室";
        }

        // ---- 关键词 ----
        for (String k : KEYWORDS) {
            if (q.contains(k)) {
                intent.keyword = k;
                break;
            }
        }

        return intent;
    }

    // ===================== 房源检索 =====================

    private List<Room> searchRooms(Intent intent) {
        boolean hasCondition = intent.minPrice != null || intent.maxPrice != null
                || intent.address != null || intent.layout != null || intent.keyword != null;

        if (!hasCondition) {
            return topRooms(3);
        }

        List<Room> rooms = queryRooms(intent);
        if (!rooms.isEmpty()) {
            return rooms;
        }

        // 放宽：去掉价格/户型限制，尽量按区域或关键词推荐
        Intent relaxed = new Intent();
        relaxed.address = intent.address;
        relaxed.keyword = intent.keyword;
        relaxed.layout = intent.layout;
        List<Room> relaxedRooms = queryRooms(relaxed);
        if (!relaxedRooms.isEmpty()) {
            return relaxedRooms;
        }

        return topRooms(3);
    }

    private List<Room> queryRooms(Intent intent) {
        LambdaQueryWrapper<Room> w = new LambdaQueryWrapper<>();
        w.eq(Room::getStatus, 1)
                .ne(Room::getStatus, 5)
                .ge(intent.minPrice != null, Room::getPrice, intent.minPrice)
                .le(intent.maxPrice != null, Room::getPrice, intent.maxPrice)
                .like(intent.address != null, Room::getAddress, intent.address)
                .like(intent.layout != null, Room::getTitle, intent.layout);
        if (intent.keyword != null) {
            w.and(x -> x.like(Room::getTitle, intent.keyword)
                    .or().like(Room::getTags, intent.keyword)
                    .or().like(Room::getDescription, intent.keyword));
        }
        w.orderByDesc(Room::getRating)
                .orderByDesc(Room::getCreateTime)
                .last("LIMIT 3");
        return roomMapper.selectList(w);
    }

    private List<Room> topRooms(int limit) {
        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getStatus, 1)
                .ne(Room::getStatus, 5)
                .orderByDesc(Room::getRating)
                .orderByDesc(Room::getCreateTime)
                .last("LIMIT " + limit));
    }

    // ===================== 回答生成 =====================

    private String buildGreeting() {
        return "您好呀！我是「租赁助手」🤖，平台的房源信息我都掌握～\n\n"
                + "您可以告诉我：\n"
                + "· 预算，比如 \"1500元以内的房源\"\n"
                + "· 区域，比如 \"天河区的房子\"\n"
                + "· 户型，比如 \"两室一厅\"\n"
                + "· 关键词，比如 \"近地铁、精装\"\n\n"
                + "下面是平台当前的热门房源，供您参考，也可以点击推荐卡片直接查看详情～";
    }

    private String buildReplyText(Intent intent, List<Room> rooms) {
        List<String> cond = new ArrayList<>();
        if (intent.minPrice != null && intent.maxPrice != null) {
            cond.add("价格 ¥" + intent.minPrice + "-" + intent.maxPrice);
        } else if (intent.maxPrice != null) {
            cond.add("预算不超过 ¥" + intent.maxPrice);
        } else if (intent.minPrice != null) {
            cond.add("价格不低于 ¥" + intent.minPrice);
        }
        if (intent.address != null) {
            cond.add("区域「" + intent.address + "」");
        }
        if (intent.layout != null) {
            cond.add("户型「" + intent.layout + "」");
        }
        if (intent.keyword != null) {
            cond.add("「" + intent.keyword + "」");
        }

        if (rooms.isEmpty()) {
            return "暂时没有找到完全符合" + (cond.isEmpty() ? "您的需求" : String.join("、", cond))
                    + "的在租房源，建议换个条件试试，或者看看我为您推荐的这些～";
        }

        StringBuilder sb = new StringBuilder();
        if (!cond.isEmpty()) {
            sb.append("根据您的要求（").append(String.join("、", cond)).append("），我为您筛选出了 ")
                    .append(rooms.size()).append(" 套房源：\n\n");
        } else {
            sb.append("为您精选了平台当前 ").append(rooms.size()).append(" 套高评分房源：\n\n");
        }

        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            sb.append(i + 1).append(". 【").append(r.getTitle()).append("】")
                    .append(" ¥").append(r.getPrice()).append("/晚")
                    .append(" · ").append(r.getAddress() == null ? "" : r.getAddress());
            if (r.getRating() != null) {
                sb.append(" · 评分 ").append(r.getRating());
            }
            if (r.getTags() != null && !r.getTags().isBlank()) {
                sb.append("\n    标签：").append(r.getTags());
            }
            sb.append("\n\n");
        }

        sb.append("💡 点击右侧的房源卡片即可直达详情；告诉我更多偏好（区域、户型、价格），我可以帮您精准筛选～");
        return sb.toString();
    }

    // ===================== 数据映射 =====================

    private Map<String, Object> toMap(Room r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("title", r.getTitle());
        m.put("address", r.getAddress());
        m.put("price", r.getPrice());
        m.put("cover", r.getCover());
        m.put("rating", r.getRating());
        m.put("tags", r.getTags());
        return m;
    }
}

