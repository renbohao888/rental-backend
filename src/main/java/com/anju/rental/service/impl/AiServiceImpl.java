package com.anju.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.anju.rental.entity.Room;
import com.anju.rental.mapper.RoomMapper;
import com.anju.rental.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private static final List<String> DISTRICTS = Arrays.asList(
            "天河", "海珠", "越秀", "荔湾", "白云", "番禺", "黄埔", "花都", "增城", "从化", "南沙",
            "南山", "福田", "罗湖", "宝安", "龙岗", "龙华", "盐田",
            "朝阳", "海淀", "丰台", "通州", "昌平",
            "浦东", "徐汇", "静安", "黄浦", "闵行", "长宁",
            "西湖", "拱墅", "滨江", "上城", "余杭"
    );

    private static final List<String> KEYWORDS = Arrays.asList(
            "地铁", "精装", "南向", "电梯", "拎包入住", "家具", "家电", "可短租", "独卫", "近商圈"
    );

    @Autowired
    private RoomMapper roomMapper;

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    private static class Intent {
        Integer minPrice;
        Integer maxPrice;
        String address;
        String layout;
        String keyword;
    }

    private boolean isAiAvailable() {
        return chatClientBuilder != null;
    }

    @Override
    public Map<String, Object> chat(String message) {
        String q = message == null ? "" : message.trim();
        log.info("AI receive: {}", q.length() > 100 ? q.substring(0, 100) + "..." : q);
        Map<String, Object> result = new HashMap<>();

        if (isGreeting(q)) {
            result.put("reply", isAiAvailable() ? buildAiGreeting() : buildGreeting());
            result.put("rooms", topRooms(3).stream().map(this::toMap).collect(Collectors.toList()));
            return result;
        }

        Intent intent = parseIntent(q);
        List<Room> rooms = searchRooms(intent);

        String reply;
        if (isAiAvailable()) {
            reply = buildAiReply(q, intent, rooms);
        } else {
            reply = buildReplyText(intent, rooms);
        }

        result.put("reply", reply);
        result.put("rooms", rooms.stream().map(this::toMap).collect(Collectors.toList()));
        log.info("AI return {} rooms", rooms.size());
        return result;
    }

    private String buildAiGreeting() {
        try {
            return chatClientBuilder.build().prompt()
                    .system("你是短期房屋租赁管理系统的智能助手，请用热情友好的语气欢迎用户。回复200字以内。")
                    .user("你好").call().content();
        } catch (Exception e) {
            log.warn("AI greeting failed: {}", e.getMessage());
            return buildGreeting();
        }
    }

    private String buildAiReply(String userMsg, Intent intent, List<Room> rooms) {
        try {
            StringBuilder ctx = new StringBuilder();
            ctx.append("检索到 ").append(rooms.size()).append(" 套房源：\n");
            for (int i = 0; i < Math.min(rooms.size(), 5); i++) {
                Room r = rooms.get(i);
                ctx.append(i + 1).append(". ").append(r.getTitle())
                        .append(" | price=").append(r.getPrice()).append("/晚")
                        .append(" | addr=").append(r.getAddress())
                        .append(" | rating=").append(r.getRating())
                        .append(" | tags=").append(r.getTags()).append("\n");
            }
            return chatClientBuilder.build().prompt()
                    .system("你是短期房屋租赁管理系统的智能助手。根据真实房源数据生成推荐回复，口语化、突出卖点，300字以内。\n房源数据：\n" + ctx)
                    .user(userMsg).call().content();
        } catch (Exception e) {
            log.warn("AI reply failed: {}", e.getMessage());
            return buildReplyText(intent, rooms);
        }
    }

    private boolean isGreeting(String q) {
        if (q.length() > 12) return false;
        return q.matches(".*(你好|您好|嗨|哈喽|hello|hi|hey|在吗|有人吗|早上好|晚上好|下午好).*");
    }

    private Intent parseIntent(String q) {
        Intent intent = new Intent();

        Matcher range = Pattern.compile("(\\d+)\\s*(?:[到至~]|-[\\s]*)\\s*(\\d+)\\s*(?:元|块)?").matcher(q);
        if (range.find()) {
            intent.minPrice = Integer.parseInt(range.group(1));
            intent.maxPrice = Integer.parseInt(range.group(2));
            if (intent.minPrice > intent.maxPrice) { int t = intent.minPrice; intent.minPrice = intent.maxPrice; intent.maxPrice = t; }
        }

        if (intent.maxPrice == null) {
            Matcher upper = Pattern.compile("(?:不超过|预算|(?:在|是)?)(\\d{3,5})(?:元|块)?(?:以内|以下|之内)?").matcher(q);
            if (upper.find()) intent.maxPrice = Integer.parseInt(upper.group(1));
        }

        if (intent.minPrice == null) {
            Matcher lower = Pattern.compile("(\\d{3,5})(?:元|块)?(?:以上|左右)").matcher(q);
            if (lower.find() && intent.maxPrice == null) intent.minPrice = Integer.parseInt(lower.group(1));
        }

        for (String d : DISTRICTS) { if (q.contains(d)) { intent.address = d; break; } }

        Matcher layout = Pattern.compile("(一室一厅|两室一厅|三室一厅|四室一厅|一居|两居|三居|四居|一房|两房|三房|四房|一室|两室|三室|四室)").matcher(q);
        if (layout.find()) intent.layout = layout.group();

        for (String kw : KEYWORDS) { if (q.contains(kw)) { intent.keyword = kw; break; } }

        log.debug("Intent: price=[{},{}], addr={}, layout={}, kw={}", intent.minPrice, intent.maxPrice, intent.address, intent.layout, intent.keyword);
        return intent;
    }

    private List<Room> searchRooms(Intent intent) {
        if (intent.minPrice == null && intent.maxPrice == null && intent.address == null && intent.layout == null && intent.keyword == null) {
            return topRooms(3);
        }
        LambdaQueryWrapper<Room> w = new LambdaQueryWrapper<>();
        w.eq(Room::getStatus, 1);
        if (intent.minPrice != null) w.ge(Room::getPrice, intent.minPrice);
        if (intent.maxPrice != null) w.le(Room::getPrice, intent.maxPrice);
        if (intent.address != null) w.like(Room::getAddress, intent.address);
        if (intent.layout != null) w.and(w2 -> w2.like(Room::getTitle, intent.layout).or().like(Room::getTags, intent.layout));
        if (intent.keyword != null) w.and(w2 -> w2.like(Room::getTags, intent.keyword).or().like(Room::getDescription, intent.keyword));
        w.orderByDesc(Room::getRating).orderByDesc(Room::getCreateTime).last("LIMIT 3");
        return roomMapper.selectList(w);
    }

    private List<Room> topRooms(int limit) {
        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getStatus, 1).ne(Room::getStatus, 5)
                .orderByDesc(Room::getRating).orderByDesc(Room::getCreateTime).last("LIMIT " + limit));
    }

    private String buildGreeting() {
        return "您好！我是短期租赁助手，平台房源信息我都掌握~\n\n" +
                "您可以告诉我：\n· 预算，比如“1500元以内的房源”\n" +
                "· 区域，比如“天河区的房子”\n· 户型，比如“两室一厅”\n" +
                "· 关键词，比如“近地铁、精装”\n\n下面是平台当前的热门房源：";
    }

    private String buildReplyText(Intent intent, List<Room> rooms) {
        List<String> cond = new ArrayList<>();
        if (intent.minPrice != null && intent.maxPrice != null) cond.add("price " + intent.minPrice + "-" + intent.maxPrice);
        else if (intent.maxPrice != null) cond.add("under " + intent.maxPrice);
        else if (intent.minPrice != null) cond.add("over " + intent.minPrice);
        if (intent.address != null) cond.add(intent.address);
        if (intent.layout != null) cond.add(intent.layout);
        if (intent.keyword != null) cond.add(intent.keyword);

        if (rooms.isEmpty()) return "暂未找到符合" + (cond.isEmpty() ? "条件" : String.join(", ", cond)) + "的房源，换个条件试试~";

        StringBuilder sb = new StringBuilder();
        if (!cond.isEmpty()) sb.append("根据（").append(String.join(", ", cond)).append("）为您找到 ").append(rooms.size()).append(" 套房源：\n\n");
        else sb.append("为您精选 ").append(rooms.size()).append(" 套高评分房源：\n\n");

        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            sb.append(i + 1).append(". 【").append(r.getTitle()).append("】 ").append(r.getPrice()).append("元/晚 · ").append(r.getAddress());
            if (r.getRating() != null) sb.append(" · ").append(r.getRating()).append("分");
            if (r.getTags() != null && !r.getTags().isBlank()) sb.append("\n   标签：").append(r.getTags());
            sb.append("\n\n");
        }
        sb.append("点击卡片直达详情，告诉我更多偏好我可以精准筛选~");
        return sb.toString();
    }

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
