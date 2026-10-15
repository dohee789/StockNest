package dev.pomyharry.stocksimulator.back.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "my_stock")
public class MyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mystock_id", updatable = false, nullable = false)
    private String id;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "total_buy_price", nullable = false)
    private long totalBuyPrice;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(name = "stock_name", nullable = false)
    private String stockName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
