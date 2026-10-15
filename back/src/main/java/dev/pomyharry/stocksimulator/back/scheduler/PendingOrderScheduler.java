package dev.pomyharry.stocksimulator.back.scheduler;

import dev.pomyharry.stocksimulator.back.service.PendingOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderScheduler {

    private final PendingOrderService pendingOrderService;

    /**
     * 30초마다 PENDING 지정가 주문 체결 조건 확인
     */
    @Scheduled(fixedDelay = 30000)
    public void checkPendingOrders() {
        try {
            pendingOrderService.checkAndExecuteAll();
        } catch (Exception e) {
            log.error("지정가 주문 스케줄러 오류: {}", e.getMessage());
        }
    }
}
