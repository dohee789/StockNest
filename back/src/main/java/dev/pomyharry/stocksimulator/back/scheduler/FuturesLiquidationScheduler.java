package dev.pomyharry.stocksimulator.back.scheduler;

import dev.pomyharry.stocksimulator.back.service.FuturesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FuturesLiquidationScheduler {

    private final FuturesService futuresService;

    /**
     * 60초마다 OPEN 포지션의 강제청산(로스컷) 조건 확인
     */
    @Scheduled(fixedDelay = 60000)
    public void checkLiquidations() {
        try {
            futuresService.checkLiquidations();
        } catch (Exception e) {
            log.error("선물 강제청산 스케줄러 오류: {}", e.getMessage());
        }
    }
}
