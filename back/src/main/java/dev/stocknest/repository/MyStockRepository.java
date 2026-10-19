package dev.stocknest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import dev.stocknest.model.entity.MyStock;
import dev.stocknest.model.entity.Customer;

public interface MyStockRepository extends JpaRepository<MyStock, String> {

    MyStock findByCustomerIdAndStockCode(String id, String stockCode);

    List<MyStock> findAllByCustomerId(String id);

    @Transactional
    void deleteAllByCustomer(Customer customer);

    @Transactional
    void deleteByCustomerId(String customerId);

}
