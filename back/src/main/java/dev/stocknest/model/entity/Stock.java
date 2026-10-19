package dev.stocknest.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @Column(name = "stock_code", nullable = false)
    private String code;

    @Column(name = "stock_name", nullable = false)
    private String name;
}
