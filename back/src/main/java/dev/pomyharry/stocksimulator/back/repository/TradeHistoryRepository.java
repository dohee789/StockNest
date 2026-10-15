package dev.pomyharry.stocksimulator.back.repository;

import dev.pomyharry.stocksimulator.back.model.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory, String> {

    @Query("SELECT t FROM TradeHistory t WHERE t.customer.id = :customerId ORDER BY t.tradeTime DESC")
    List<TradeHistory> findAllByCustomerIdOrderByTradeTimeDesc(@Param("customerId") String customerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM TradeHistory t WHERE t.customer.id = :customerId")
    void deleteAllByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT t FROM TradeHistory t WHERE t.customer.id = :customerId AND t.tradeTime >= :from AND t.tradeTime < :to")
    List<TradeHistory> findByCustomerIdAndTradeDateRange(
            @Param("customerId") String customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT DISTINCT t.customer FROM TradeHistory t WHERE t.tradeTime >= :from AND t.tradeTime < :to")
    List<dev.pomyharry.stocksimulator.back.model.entity.Customer> findCustomersWithTradesInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
