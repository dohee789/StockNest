package dev.pomyharry.stocksimulator.back.repository;

import dev.pomyharry.stocksimulator.back.model.entity.PendingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PendingOrderRepository extends JpaRepository<PendingOrder, String> {

    @Query("SELECT p FROM PendingOrder p JOIN FETCH p.customer WHERE p.customer.id = :customerId ORDER BY p.createdAt DESC")
    List<PendingOrder> findAllByCustomerIdOrderByCreatedAtDesc(@Param("customerId") String customerId);

    // 스케줄러용: 전체 PENDING 주문 + customer 즉시 로딩
    @Query("SELECT p FROM PendingOrder p JOIN FETCH p.customer WHERE p.status = 'PENDING'")
    List<PendingOrder> findAllPendingWithCustomer();
}
