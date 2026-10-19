package dev.stocknest.scheduler;

import dev.stocknest.component.StockDataComponent;
import dev.stocknest.model.dto.StockDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class StockPriceScheduler {

    private final SimpMessagingTemplate messagingTemplate;
    private final StockDataComponent stockDataComponent;

    // 구독된 종목 코드들 (thread-safe)
    private final CopyOnWriteArraySet<String> subscribedStocks = new CopyOnWriteArraySet<>();

    public StockPriceScheduler(SimpMessagingTemplate messagingTemplate,
                                StockDataComponent stockDataComponent) {
        this.messagingTemplate = messagingTemplate;
        this.stockDataComponent = stockDataComponent;
    }

    public void addSubscription(String stockCode) {
        subscribedStocks.add(stockCode);
    }

    public void removeSubscription(String stockCode) {
        subscribedStocks.remove(stockCode);
    }

    // 30초마다 구독된 종목의 시세를 WebSocket으로 브로드캐스트
    @Scheduled(fixedDelay = 30000)
    public void broadcastStockPrices() {
        if (subscribedStocks.isEmpty()) return;

        List<StockDTO> prices = new ArrayList<>();
        for (String code : subscribedStocks) {
            try {
                StockDTO dto = stockDataComponent.getStockChart(code, code);
                if (dto != null) {
                    prices.add(dto);
                }
            } catch (Exception e) {
                // 개별 종목 조회 실패는 무시
            }
        }

        if (!prices.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/stock-prices", prices);
        }
    }
}
