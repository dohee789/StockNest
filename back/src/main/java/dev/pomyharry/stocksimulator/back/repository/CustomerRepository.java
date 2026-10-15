package dev.pomyharry.stocksimulator.back.repository;

import dev.pomyharry.stocksimulator.back.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);
}
