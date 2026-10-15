package dev.pomyharry.stocksimulator.back.service;

import dev.pomyharry.stocksimulator.back.model.dto.AccountDTO;
import dev.pomyharry.stocksimulator.back.model.entity.Account;
import dev.pomyharry.stocksimulator.back.model.entity.Customer;
import dev.pomyharry.stocksimulator.back.repository.AccountRepository;
import dev.pomyharry.stocksimulator.back.repository.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerService customerService;
    private final MyStockService myStockService;
    private final TradeHistoryRepository tradeHistoryRepository;

    @Override
    public void insertAccount(AccountDTO dto) {
        Customer customer = customerService.findById(dto.getCustomerId());
        Account account = Account.builder()
                .name(dto.getName())
                .deposit(dto.getDeposit())
                .seedMoney(dto.getSeedMoney())
                .customer(customer)
                .build();
        accountRepository.save(account);
    }

    @Override
    public Account findByCustomerId(AccountDTO dto) {
        return accountRepository.findByCustomerId(dto.getCustomerId());
    }

    @Override
    public Account updateAccount(AccountDTO dto) {
        Account account = accountRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("계좌를 찾을 수 없습니다."));
        account.setName(dto.getName());
        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deleteAccount(AccountDTO dto) {
        myStockService.deleteByCustomerId(dto.getCustomerId());
        accountRepository.deleteById(dto.getId());
    }

    @Override
    @Transactional
    public void deleteAllAccount(Customer customer) {
        myStockService.deleteAllByCustomer(customer);
        accountRepository.deleteAllByCustomer(customer);
    }

    @Override
    @Transactional
    public void resetAccount(String customerId) {
        Account account = accountRepository.findByCustomerId(customerId);
        if (account == null) throw new RuntimeException("계좌가 존재하지 않습니다.");
        myStockService.deleteByCustomerId(customerId);
        tradeHistoryRepository.deleteAllByCustomerId(customerId);
        account.setDeposit(account.getSeedMoney());
        accountRepository.save(account);
    }
}
