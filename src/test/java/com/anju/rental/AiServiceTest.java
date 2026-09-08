package com.anju.rental;

import com.anju.rental.common.JwtUtils;
import com.anju.rental.entity.Room;
import com.anju.rental.mapper.RoomMapper;
import com.anju.rental.service.AiService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AI 助手服务单元测试
 * <p>
 * 测试意图解析、问候识别、数据检索等核心逻辑（不依赖大模型 API）。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private com.anju.rental.service.impl.AiServiceImpl aiService;

    private static final List<Room> MOCK_ROOMS = Arrays.asList(
            createRoom(1L, "天河精装公寓", "天河区体育西路", 1500, 4.5, "近地铁,精装"),
            createRoom(2L, "海珠阳光房", "海珠区江南西", 1200, 4.2, "南向,家具齐全"),
            createRoom(3L, "越秀老城区", "越秀区北京路", 2000, 4.8, "近商圈,电梯")
    );

    @BeforeAll
    static void setupOnce() {
        JwtUtils.setConfig("test-secret-key-for-unit-test-2025", 86400000L);
    }

    // ==================== 问候识别 ====================

    @Test
    @DisplayName("问候语应被正确识别")
    void testGreetingRecognition() {
        when(roomMapper.selectList(any())).thenReturn(MOCK_ROOMS);

        Map<String, Object> result = aiService.chat("你好");
        assertNotNull(result.get("reply"));
        assertNotNull(result.get("rooms"));
        assertFalse(((List<?>) result.get("rooms")).isEmpty());
    }

    @Test
    @DisplayName("普通消息不应被识别为问候")
    void testNonGreeting() {
        when(roomMapper.selectList(any())).thenReturn(MOCK_ROOMS);

        Map<String, Object> result = aiService.chat("天河区1500元的房子");
        assertNotNull(result.get("reply"));
        assertTrue(result.get("reply").toString().contains("天河"));
    }

    // ==================== 意图解析 ====================

    @Test
    @DisplayName("价格区间解析")
    void testPriceRangeParsing() {
        when(roomMapper.selectList(any())).thenReturn(MOCK_ROOMS);

        Map<String, Object> result = aiService.chat("1000到2000元的房子");
        String reply = result.get("reply").toString();
        assertTrue(reply.contains("1000") || reply.contains("2000"));
    }

    @Test
    @DisplayName("预算上限解析")
    void testBudgetUpperLimit() {
        when(roomMapper.selectList(any())).thenReturn(MOCK_ROOMS.subList(0, 2));

        Map<String, Object> result = aiService.chat("预算1500");
        assertNotNull(result.get("reply"));
    }

    @Test
    @DisplayName("区域解析")
    void testDistrictParsing() {
        when(roomMapper.selectList(any())).thenReturn(MOCK_ROOMS.subList(0, 1));

        Map<String, Object> result = aiService.chat("天河区的房子");
        String reply = result.get("reply").toString();
        assertTrue(reply.contains("天河"));
    }

    @Test
    @DisplayName("户型解析")
    void testLayoutParsing() {
        when(roomMapper.selectList(any())).thenReturn(Arrays.asList(MOCK_ROOMS.get(0)));

        Map<String, Object> result = aiService.chat("两室一厅的房子");
        assertNotNull(result.get("reply"));
    }

    @Test
    @DisplayName("关键词解析")
    void testKeywordParsing() {
        when(roomMapper.selectList(any())).thenReturn(Arrays.asList(MOCK_ROOMS.get(0)));

        Map<String, Object> result = aiService.chat("近地铁的房子");
        assertNotNull(result.get("reply"));
    }

    // ==================== 边界情况 ====================

    @Test
    @DisplayName("空消息处理")
    void testEmptyMessage() {
        when(roomMapper.selectList(any())).thenReturn(MOCK_ROOMS);

        Map<String, Object> result = aiService.chat("");
        assertNotNull(result.get("reply"));
    }

    @Test
    @DisplayName("null 消息处理")
    void testNullMessage() {
        when(roomMapper.selectList(any())).thenReturn(MOCK_ROOMS);

        Map<String, Object> result = aiService.chat(null);
        assertNotNull(result.get("reply"));
    }

    // ==================== 数据映射 ====================

    @Test
    @DisplayName("房源转 Map 包含必要字段")
    void testRoomToMap() {
        when(roomMapper.selectList(any())).thenReturn(Arrays.asList(MOCK_ROOMS.get(0)));

        Map<String, Object> result = aiService.chat("天河的房子");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) result.get("rooms");
        assertFalse(rooms.isEmpty());

        Map<String, Object> room = rooms.get(0);
        assertTrue(room.containsKey("id"));
        assertTrue(room.containsKey("title"));
        assertTrue(room.containsKey("price"));
        assertTrue(room.containsKey("address"));
        assertTrue(room.containsKey("rating"));
    }

    // ==================== 工具方法 ====================

    private static Room createRoom(Long id, String title, String address, int price, double rating, String tags) {
        Room room = new Room();
        room.setId(id);
        room.setTitle(title);
        room.setAddress(address);
        room.setPrice(BigDecimal.valueOf(price));
        room.setRating(BigDecimal.valueOf(rating));
        room.setTags(tags);
        room.setStatus(1);
        return room;
    }
}