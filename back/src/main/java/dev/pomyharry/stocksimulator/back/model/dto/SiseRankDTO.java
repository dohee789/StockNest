package dev.pomyharry.stocksimulator.back.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiseRankDTO {
    private String code;
    private String name;
    private String price;
    private String change;
    private String changeRate;
    private String volume;
}
