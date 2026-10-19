package dev.stocknest.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id", updatable = false, nullable = false)
    private String id;

    @Column(name = "account_name", nullable = false)
    private String name;

    @Column(name = "deposit", nullable = false)
    private long deposit;

    @Column(name = "seed_money", nullable = false)
    private long seedMoney;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
