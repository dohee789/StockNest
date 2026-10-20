package dev.stocknest.scheduler;

import dev.stocknest.model.entity.Customer;
import dev.stocknest.model.entity.MyStock;
import dev.stocknest.repository.CustomerRepository;
import dev.stocknest.repository.MyStockRepository;
import dev.stocknest.service.ChatbotService;
import dev.stocknest.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTradeSignalScheduler {

    private final CustomerRepository customerRepository;
    private final MyStockRepository myStockRepository;
    private final ChatbotService chatbotService;
    private final NotificationService notificationService;

    /**
     * 매 평일 14:00 KST — 보유 종목이 있는 사용자에게 AI 매매 신호 알림
     */
    @Scheduled(cron = "0 0 14 * * MON-FRI", zone = "Asia/Seoul")
    public void sendTradeSignals() {
        List<Customer> customers = customerRepository.findAll();
        log.info("AI 매매 신호 분석 시작: 전체 고객 {}명", customers.size());

        for (Customer customer : customers) {
            try {
                List<MyStock> stocks = myStockRepository.findAllByCustomerId(customer.getId());
                if (stocks.isEmpty()) continue;

                String signals = chatbotService.generateTradeSignals(customer.getId());
                if (signals != null && !signals.isBlank()
                        && !signals.contains("보유 종목이 없어")) {
                    notificationService.sendAiInsightNotification(customer,
                            "AI 매매 신호\n" + signals);
                    log.info("AI 매매 신호 발송 완료: {}", customer.getId());
                }

                // Gemini API 속도 제한 방지
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("AI 매매 신호 발송 실패 [{}]: {}", customer.getId(), e.getMessage());
            }
        }
    }
}
