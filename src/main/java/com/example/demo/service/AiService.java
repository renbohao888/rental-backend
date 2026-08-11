package com.example.demo.service;

import java.util.Map;

/**
 * 租赁助手 AI 服务
 */
public interface AiService {

    /**
     * 根据用户输入生成智能回答
     *
     * @param message 用户输入
     * @return map，包含 reply（回答文本）和 rooms（推荐房源列表）
     */
    Map<String, Object> chat(String message);
}
