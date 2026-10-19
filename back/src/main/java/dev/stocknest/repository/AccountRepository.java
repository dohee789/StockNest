package dev.stocknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import dev.stocknest.model.entity.Account;
import dev.stocknest.model.entity.Customer;

public interface AccountRepository extends JpaRepository<Account, String> {
    
    Account findByCustomerId(String id);

    @Transactional
    void deleteAllByCustomer(Customer customer);
}
