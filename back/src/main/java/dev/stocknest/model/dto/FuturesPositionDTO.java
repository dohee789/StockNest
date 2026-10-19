package dev.stocknest.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuturesPositionDTO {
    private String id;
    private String futuresCode;
    private String futuresName;
    private String direction;       // LONG | SHORT
    private long marginAmount;      // 투입 증거금 (KRW)
    private int leverage;
    private double entryPrice;      // 진입 가격 (native currency)
    private Double currentPrice;    // 현재 가격 (native currency)
    private String currency;        // USD | KRW
    private Long unrealizedPnl;     // 평가손익 (KRW)
    private double unrealizedPnlRate; // 수익률 % (증거금 대비)
    private String status;          // OPEN | CLOSED | LIQUIDATED
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private Long realizedPnl;       // 실현손익 (KRW, 청산 후)
}
