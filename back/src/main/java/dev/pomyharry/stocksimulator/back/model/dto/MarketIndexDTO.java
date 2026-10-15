package dev.pomyharry.stocksimulator.back.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndexDTO {
    private String name;       // KOSPI, KOSDAQ
    private String value;      // 지수값
    private String change;     // 등락
    private String changeRate; // 등락율
    private boolean isUp;      // 상승여부
}
