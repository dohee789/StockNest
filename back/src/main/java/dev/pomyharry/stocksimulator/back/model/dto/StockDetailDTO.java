package dev.pomyharry.stocksimulator.back.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDetailDTO {
    private String code;
    private String name;
    private String price;
    private String change;
    private String changeRate;
    private String volume;       // 거래량
    private String high52w;      // 52주 최고
    private String low52w;       // 52주 최저
    private String per;          // PER
    private String pbr;          // PBR
    private List<CandleDTO> candles;
}
