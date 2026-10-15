package dev.pomyharry.stocksimulator.back.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pending_order")
public class PendingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private String id;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(name = "stock_name", nullable = false)
    private String stockName;

    /**
     * BUY | SELL
     */
    @Column(name = "order_type", nullable = false)
    private String orderType;

    /**
     * PRICE_BELOW : 현재가 ≤ targetPrice 일 때 체결 (지정가 매수, 손절 매도)
     * PRICE_ABOVE : 현재가 ≥ targetPrice 일 때 체결 (익절 매도)
     */
    @Column(name = "condition_type", nullable = false)
    private String conditionType;

    @Column(name = "target_price", nullable = false)
    private long targetPrice;

    @Column(name = "amount", nullable = false)
    private int amount;

    /**
     * PENDING | EXECUTED | CANCELLED
     */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
