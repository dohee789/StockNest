package dev.pomyharry.stocksimulator.back.repository;

import dev.pomyharry.stocksimulator.back.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("SELECT m FROM ChatMessage m WHERE m.customer.id = :customerId ORDER BY m.createdAt ASC")
    List<ChatMessage> findAllByCustomerIdOrderByCreatedAtAsc(@Param("customerId") String customerId);

    @Query("SELECT m FROM ChatMessage m WHERE m.customer.id = :customerId ORDER BY m.createdAt DESC")
    List<ChatMessage> findAllByCustomerIdOrderByCreatedAtDesc(@Param("customerId") String customerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.customer.id = :customerId")
    void deleteAllByCustomerId(@Param("customerId") String customerId);
}
