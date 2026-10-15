package dev.pomyharry.stocksimulator.back.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuturesOrderDTO {
    private String customerId;
    private String futuresCode;  // "NASDAQ100", "SP500", etc.
    private String direction;    // "LONG" | "SHORT"
    private long marginAmount;   // 증거금 (KRW)
    private int leverage;        // 2 | 5 | 10 | 20
}
