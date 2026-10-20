package dev.stocknest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRecommendationDTO {
    private String stockCode;
    private String stockName;
    private String reason;
    private String confidence; // high, medium, low
    private String action;     // buy, watch
    private String currentPrice;
    private String changeRate;
}
