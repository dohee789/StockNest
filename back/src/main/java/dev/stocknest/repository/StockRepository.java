package dev.stocknest.repository;

import dev.stocknest.model.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, String> {

    Optional<Stock> findByCode(String code);

    List<Stock> findByNameContainingIgnoreCase(String name);
}
