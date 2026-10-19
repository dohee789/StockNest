package dev.stocknest.repository;

import dev.stocknest.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    @Query("SELECT n FROM Notification n WHERE n.customer.id = :customerId ORDER BY n.createdAt DESC")
    List<Notification> findAllByCustomerIdOrderByCreatedAtDesc(@Param("customerId") String customerId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.customer.id = :customerId AND n.read = false")
    long countUnreadByCustomerId(@Param("customerId") String customerId);

    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.customer.id = :customerId AND n.read = false")
    void markAllReadByCustomerId(@Param("customerId") String customerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.customer.id = :customerId")
    void deleteAllByCustomerId(@Param("customerId") String customerId);
}
