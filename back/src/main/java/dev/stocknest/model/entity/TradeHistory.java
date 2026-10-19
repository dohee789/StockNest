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
@Table(name = "trade_history")
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trade_id", updatable = false, nullable = false)
    private String id;

    @Column(name = "trade_type", nullable = false)
    private String tradeType; // BUY or SELL

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(name = "stock_name", nullable = false)
    private String stockName;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "price_per_stock", nullable = false)
    private long pricePerStock;

    @Column(name = "total_price", nullable = false)
    private long totalPrice;

    @Column(name = "trade_time", nullable = false)
    private LocalDateTime tradeTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
