package com.example.demo.controller;

import com.example.demo.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 租赁助手 AI 接口（游客可直接使用）
 */
@CrossOrigin
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * AI 对话：根据用户输入返回推荐房源与回答文本
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        String message = params == null ? null : params.get("message");
        if (message == null || message.trim().isEmpty()) {
            result.put("code", 400);
            result.put("message", "请输入您想咨询的内容");
            result.put("data", null);
            return result;
        }
        Map<String, Object> data = aiService.chat(message);
        result.put("code", 200);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }
}
