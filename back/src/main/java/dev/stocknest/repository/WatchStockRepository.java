package dev.stocknest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import dev.stocknest.model.entity.WatchStock;
import dev.stocknest.model.entity.Customer;

public interface WatchStockRepository extends JpaRepository<WatchStock, String> {

    WatchStock findByStockCodeAndCustomerId(String stockCode, String customerId);

    @Transactional
    void deleteAllByCustomer(Customer customer);

    List<WatchStock> findAllByCustomerId(String customerId);

    @Transactional
    void deleteByStockCode(String code);
}
