package dev.pomyharry.stocksimulator.back.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuturesItemDTO {
    private String code;        // 내부 코드: "NASDAQ100", "SP500", etc.
    private String name;        // 표시명: "나스닥100 선물"
    private String category;    // "INDEX" | "COMMODITY"
    private String currency;    // "USD" | "KRW"
    private String priceUnit;   // "pt" | "USD/oz" | "USD/bbl"
    private String yahooSymbol; // Yahoo Finance 티커 (내부용)
    private Double currentPrice;
    private Double change;
    private Double changeRate;
    private boolean available;  // 가격 조회 성공 여부
}
