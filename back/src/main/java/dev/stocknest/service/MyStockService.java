package dev.stocknest.service;

import java.util.List;

import dev.stocknest.model.dto.MyStockDTO;
import dev.stocknest.model.entity.Customer;

public interface MyStockService {

    void buyStock(MyStockDTO myStockDTO);

    void sellStock(MyStockDTO myStockDTO);

    List<MyStockDTO> findAllMyStockByCustomerId(String customerId);

    void deleteAllByCustomer(Customer customer);

    void deleteByCustomerId(String customerId);
}
