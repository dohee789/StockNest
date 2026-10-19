package dev.stocknest.controller;

import dev.stocknest.model.dto.ChatMessageDTO;
import dev.stocknest.model.dto.ChatRequestDTO;
import dev.stocknest.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /** 일반 챗봇 (비스트리밍) */
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequestDTO request) {
        try {
            ChatMessageDTO response = chatbotService.chat(request.getCustomerId(), request.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 스트리밍 챗봇 (SSE) */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @RequestParam String customerId,
            @RequestParam String message) {
        return chatbotService.chatStream(customerId, message);
    }

    /** 종목 AI 분석 */
    @PostMapping("/stock-analysis")
    public ResponseEntity<?> analyzeStock(@RequestBody Map<String, String> request) {
        try {
            String analysis = chatbotService.analyzeStock(
                    request.get("customerId"), request.get("stockCode"));
            return ResponseEntity.ok(Map.of("analysis", analysis));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 포트폴리오 AI 리포트 */
    @PostMapping("/portfolio-report")
    public ResponseEntity<?> portfolioReport(@RequestBody Map<String, String> request) {
        try {
            String report = chatbotService.generatePortfolioReport(request.get("customerId"));
            return ResponseEntity.ok(Map.of("report", report));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** AI 마켓 인사이트 */
    @PostMapping("/market-insight")
    public ResponseEntity<?> marketInsight(@RequestBody Map<String, String> request) {
        try {
            String insight = chatbotService.generateMarketInsight(request.get("customerId"));
            return ResponseEntity.ok(Map.of("insight", insight));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 대화 기록 조회 */
    @GetMapping("/history/{customerId}")
    public ResponseEntity<?> getHistory(@PathVariable String customerId) {
        try {
            List<ChatMessageDTO> history = chatbotService.getHistory(customerId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 대화 기록 삭제 */
    @DeleteMapping("/history/{customerId}")
    public ResponseEntity<?> clearHistory(@PathVariable String customerId) {
        try {
            chatbotService.clearHistory(customerId);
            return ResponseEntity.ok(Map.of("message", "대화 기록이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
