package dev.pomyharry.stocksimulator.back.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrderDTO {

    private String id;
    private String stockCode;
    private String stockName;
    private String orderType;       // BUY | SELL
    private String conditionType;   // PRICE_BELOW | PRICE_ABOVE
    private long targetPrice;
    private int amount;
    private String status;          // PENDING | EXECUTED | CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
    private String customerId;
}
