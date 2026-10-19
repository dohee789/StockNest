package dev.stocknest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtfItemDTO {
    private String code;           // KRX 6자리 종목코드
    private String name;           // ETF 이름
    private String category;       // DOMESTIC_INDEX, FOREIGN_INDEX, SECTOR, COMMODITY, BOND, LEVERAGE
    private String trackingIndex;  // 추적 지수/자산
    private Double currentPrice;
    private Double change;
    private Double changeRate;
    private boolean available;
}
