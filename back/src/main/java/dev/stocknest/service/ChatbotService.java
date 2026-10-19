package dev.stocknest.service;

import dev.stocknest.model.dto.ChatMessageDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatbotService {
    ChatMessageDTO chat(String customerId, String userMessage);
    SseEmitter chatStream(String customerId, String userMessage);
    String analyzeStock(String customerId, String stockCode);
    String generatePortfolioReport(String customerId);
    String generateMarketInsight(String customerId);
    List<ChatMessageDTO> getHistory(String customerId);
    void clearHistory(String customerId);
}
