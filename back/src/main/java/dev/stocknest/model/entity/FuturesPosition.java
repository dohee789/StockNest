package dev.stocknest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "futures_position")
public class FuturesPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "position_id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "futures_code", nullable = false)
    private String futuresCode;

    @Column(name = "futures_name", nullable = false)
    private String futuresName;

    /**
     * LONG | SHORT
     */
    @Column(name = "direction", nullable = false)
    private String direction;

    /**
     * 투입 증거금 (KRW). 계좌에서 차감됨.
     * P&L = marginAmount * leverage * priceChangePct * (LONG=+1 / SHORT=-1)
     */
    @Column(name = "margin_amount", nullable = false)
    private long marginAmount;

    @Column(name = "leverage", nullable = false)
    private int leverage;

    /**
     * 진입 시점의 가격 (native currency, e.g. USD or KRW pt)
     */
    @Column(name = "entry_price", nullable = false)
    private double entryPrice;

    /**
     * OPEN | CLOSED | LIQUIDATED
     */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * 청산/반대매매 시 확정된 손익 (KRW)
     */
    @Column(name = "realized_pnl")
    private Long realizedPnl;
}
