package dev.pomyharry.stocksimulator.back.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistoryDTO {
    private String id;
    private String tradeType;       // BUY or SELL
    private String stockCode;
    private String stockName;
    private int amount;
    private long pricePerStock;
    private long totalPrice;
    private LocalDateTime tradeTime;
    private String customerId;
}
