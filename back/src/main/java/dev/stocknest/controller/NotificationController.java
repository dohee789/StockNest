package dev.stocknest.controller;

import dev.stocknest.model.dto.NotificationDTO;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.repository.CustomerRepository;
import dev.stocknest.service.ChatbotService;
import dev.stocknest.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final ChatbotService chatbotService;
    private final CustomerRepository customerRepository;

    @GetMapping("/{customerId}")
    public ResponseEntity<List<NotificationDTO>> getNotifications(@PathVariable String customerId) {
        return ResponseEntity.ok(notificationService.getNotifications(customerId));
    }

    @GetMapping("/{customerId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String customerId) {
        long count = notificationService.getUnreadCount(customerId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{customerId}/mark-read")
    public ResponseEntity<Void> markAllRead(@PathVariable String customerId) {
        notificationService.markAllRead(customerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{customerId}/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String customerId,
                                                    @PathVariable String notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteAllNotifications(@PathVariable String customerId) {
        notificationService.deleteAllByCustomerId(customerId);
        return ResponseEntity.ok().build();
    }

    /** AI 마켓 인사이트 생성 → 알림으로 저장 */
    @PostMapping("/{customerId}/ai-insight")
    public ResponseEntity<?> generateAiInsight(@PathVariable String customerId) {
        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));
            String insight = chatbotService.generateMarketInsight(customerId);
            notificationService.sendAiInsightNotification(customer, insight);
            return ResponseEntity.ok(Map.of("message", "AI 인사이트가 생성되었습니다.", "insight", insight));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
