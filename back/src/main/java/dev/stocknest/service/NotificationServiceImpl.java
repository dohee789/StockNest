package dev.stocknest.service;

import dev.stocknest.model.dto.NotificationDTO;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.model.entity.Notification;
import dev.stocknest.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final KakaoMessageService kakaoMessageService;

    @Override
    @Transactional
    public void sendTradeNotification(Customer customer, String tradeType, String orderType,
                                      String stockName, int amount, long pricePerStock, long totalPrice) {
        boolean isBuy = "BUY".equals(tradeType);
        boolean isLimit = "LIMIT".equals(orderType);

        String type = (isBuy ? "BUY" : "SELL") + "_" + (isLimit ? "LIMIT" : "MARKET");
        String title = (isBuy ? "매수" : "매도") + " 체결 - " + stockName;
        String message = String.format("%s %d주 체결\n단가: %,d원 | 총액: %,d원",
                stockName, amount, pricePerStock, totalPrice);
        if (isLimit) {
            message = "[지정가] " + message;
        }

        Notification notification = Notification.builder()
                .customer(customer)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
        notificationRepository.save(notification);

        // 카카오 메시지 발송 (비동기 처리 - 실패해도 트랜잭션 롤백 안 함)
        final String customerId = customer.getId();
        final String finalTitle = title;
        final String finalMessage = message;
        new Thread(() -> kakaoMessageService.sendToMe(customerId, finalTitle, finalMessage)).start();
    }

    @Override
    @Transactional
    public void sendDailySummaryNotification(Customer customer, int totalTrades, long totalBuyAmount,
                                             long totalSellAmount, long netPnl) {
        String title = "오늘의 모의투자 결산";
        String pnlStr = netPnl >= 0
                ? String.format("+%,d원", netPnl)
                : String.format("%,d원", netPnl);
        String message = String.format(
                "총 거래 %d건\n매수: %,d원 | 매도: %,d원\n손익: %s",
                totalTrades, totalBuyAmount, totalSellAmount, pnlStr);

        Notification notification = Notification.builder()
                .customer(customer)
                .type("DAILY_SUMMARY")
                .title(title)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
        notificationRepository.save(notification);

        final String customerId = customer.getId();
        final String finalTitle = title;
        final String finalMessage = message;
        new Thread(() -> kakaoMessageService.sendToMe(customerId, finalTitle, finalMessage)).start();
    }

    @Override
    @Transactional
    public void sendAiInsightNotification(Customer customer, String insight) {
        Notification notification = Notification.builder()
                .customer(customer)
                .type("AI_INSIGHT")
                .title("AI 마켓 인사이트")
                .message(insight)
                .read(false)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(String customerId) {
        return notificationRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(n -> NotificationDTO.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String customerId) {
        return notificationRepository.countUnreadByCustomerId(customerId);
    }

    @Override
    @Transactional
    public void markAllRead(String customerId) {
        notificationRepository.markAllReadByCustomerId(customerId);
    }

    @Override
    @Transactional
    public void deleteNotification(String notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void deleteAllByCustomerId(String customerId) {
        notificationRepository.deleteAllByCustomerId(customerId);
    }
}
