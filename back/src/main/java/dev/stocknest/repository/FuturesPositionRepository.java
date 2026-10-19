package dev.stocknest.repository;

import dev.stocknest.model.entity.FuturesPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FuturesPositionRepository extends JpaRepository<FuturesPosition, String> {

    @Query("SELECT p FROM FuturesPosition p WHERE p.customer.id = :customerId AND p.status = :status ORDER BY p.openedAt DESC")
    List<FuturesPosition> findByCustomerIdAndStatus(@Param("customerId") String customerId,
                                                    @Param("status") String status);

    @Query("SELECT p FROM FuturesPosition p WHERE p.status = 'OPEN'")
    List<FuturesPosition> findAllOpen();

    @Query("SELECT p FROM FuturesPosition p WHERE p.customer.id = :customerId ORDER BY p.openedAt DESC")
    List<FuturesPosition> findAllByCustomerId(@Param("customerId") String customerId);
}
