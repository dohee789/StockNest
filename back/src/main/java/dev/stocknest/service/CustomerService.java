package dev.stocknest.service;

import dev.stocknest.model.dto.CustomerDTO;
import dev.stocknest.model.entity.Customer;

public interface CustomerService {

    CustomerDTO login(CustomerDTO customer);

    Customer create(Customer customer);

    Customer findById(String id);

    Customer updateCustomerInfo(CustomerDTO customer);

    void deleteCustomerInfO(CustomerDTO customer);
}
