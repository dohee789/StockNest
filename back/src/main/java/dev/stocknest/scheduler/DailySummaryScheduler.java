package dev.stocknest.scheduler;

import dev.stocknest.model.entity.Customer;
import dev.stocknest.model.entity.TradeHistory;
import dev.stocknest.repository.TradeHistoryRepository;
import dev.stocknest.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailySummaryScheduler {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final NotificationService notificationService;

    /**
     * 매 평일 15:35 KST — 장 마감 후 오늘의 거래 요약 알림 발송
     */
    @Scheduled(cron = "0 35 15 * * MON-FRI", zone = "Asia/Seoul")
    public void sendDailySummary() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        List<Customer> customers = tradeHistoryRepository.findCustomersWithTradesInRange(from, to);
        log.info("일일 요약 알림 발송 대상: {}명", customers.size());

        for (Customer customer : customers) {
            try {
                List<TradeHistory> trades = tradeHistoryRepository
                        .findByCustomerIdAndTradeDateRange(customer.getId(), from, to);

                int totalTrades = trades.size();
                long totalBuyAmount = trades.stream()
                        .filter(t -> "BUY".equals(t.getTradeType()))
                        .mapToLong(TradeHistory::getTotalPrice).sum();
                long totalSellAmount = trades.stream()
                        .filter(t -> "SELL".equals(t.getTradeType()))
                        .mapToLong(TradeHistory::getTotalPrice).sum();
                long netPnl = totalSellAmount - totalBuyAmount;

                notificationService.sendDailySummaryNotification(
                        customer, totalTrades, totalBuyAmount, totalSellAmount, netPnl);
            } catch (Exception e) {
                log.warn("일일 요약 알림 발송 실패 [{}]: {}", customer.getId(), e.getMessage());
            }
        }
    }
}
