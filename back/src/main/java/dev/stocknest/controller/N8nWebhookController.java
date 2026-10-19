package dev.stocknest.controller;

import dev.stocknest.model.entity.Customer;
import dev.stocknest.repository.CustomerRepository;
import dev.stocknest.service.ChatbotService;
import dev.stocknest.service.NotificationService;
import dev.stocknest.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * n8n 워크플로우에서 호출하는 웹훅 엔드포인트
 * n8n → HTTP Request 노드 → 이 컨트롤러 호출
 */
@Slf4j
@RestController
@RequestMapping("/api/n8n")
@RequiredArgsConstructor
public class N8nWebhookController {

    private final ChatbotService chatbotService;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final RagService ragService;

    /**
     * 전체 사용자에게 AI 마켓 인사이트 알림 발송
     * n8n 스케줄러에서 매일 오전 9시에 호출
     */
    @PostMapping("/daily-insight")
    public ResponseEntity<?> sendDailyInsight() {
        log.info("[n8n] 일일 AI 마켓 인사이트 생성 요청");
        List<Customer> customers = customerRepository.findAll();
        int sent = 0;

        for (Customer customer : customers) {
            try {
                String insight = chatbotService.generateMarketInsight(customer.getId());
                notificationService.sendAiInsightNotification(customer, insight);
                sent++;
            } catch (Exception e) {
                log.warn("[n8n] 인사이트 생성 실패: customerId={}, error={}", customer.getId(), e.getMessage());
            }
        }

        log.info("[n8n] 일일 인사이트 발송 완료: {}/{}", sent, customers.size());
        return ResponseEntity.ok(Map.of(
                "message", "일일 인사이트 발송 완료",
                "total", customers.size(),
                "sent", sent
        ));
    }

    /**
     * 특정 사용자에게 포트폴리오 리포트 알림 발송
     * n8n에서 주간 스케줄로 호출
     */
    @PostMapping("/portfolio-report/{customerId}")
    public ResponseEntity<?> sendPortfolioReport(@PathVariable String customerId) {
        log.info("[n8n] 포트폴리오 리포트 생성 요청: customerId={}", customerId);
        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));
            String report = chatbotService.generatePortfolioReport(customerId);
            notificationService.sendAiInsightNotification(customer, "📊 주간 포트폴리오 리포트\n\n" + report);
            return ResponseEntity.ok(Map.of("message", "포트폴리오 리포트 발송 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * RAG 지식 베이스 새로고침
     * n8n에서 새로운 데이터 추가 후 호출
     */
    @PostMapping("/rag/refresh")
    public ResponseEntity<?> refreshKnowledgeBase() {
        log.info("[n8n] RAG 지식 베이스 새로고침 요청");
        try {
            ragService.initializeKnowledgeBase();
            return ResponseEntity.ok(Map.of(
                    "message", "지식 베이스 새로고침 완료",
                    "ready", ragService.isReady()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * RAG 상태 확인
     */
    @GetMapping("/rag/status")
    public ResponseEntity<?> ragStatus() {
        return ResponseEntity.ok(Map.of(
                "ragReady", ragService.isReady()
        ));
    }

    /**
     * 헬스 체크 (n8n에서 서버 상태 확인용)
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "ragReady", ragService.isReady()
        ));
    }
}
