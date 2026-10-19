package dev.stocknest.service;

import dev.stocknest.model.dto.AccountDTO;
import dev.stocknest.model.entity.Account;
import dev.stocknest.model.entity.Customer;

public interface AccountService {

    void insertAccount(AccountDTO accountDTO);

    Account findByCustomerId(AccountDTO acc);

    Account updateAccount(AccountDTO acc);

    void deleteAccount(AccountDTO acc);

    void deleteAllAccount(Customer customer);

    void resetAccount(String customerId);
}
