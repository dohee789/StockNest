package dev.pomyharry.stocksimulator.back.controller;

import dev.pomyharry.stocksimulator.back.model.dto.AccountDTO;
import dev.pomyharry.stocksimulator.back.model.dto.MyStockDTO;
import dev.pomyharry.stocksimulator.back.model.entity.Account;
import dev.pomyharry.stocksimulator.back.service.AccountService;
import dev.pomyharry.stocksimulator.back.service.MyStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final MyStockService myStockService;

    @PostMapping("/account")
    public ResponseEntity<?> insertAccount(@RequestBody AccountDTO accountDTO) {
        try {
            accountService.insertAccount(accountDTO);
            return ResponseEntity.ok(accountDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/info/account")
    public ResponseEntity<?> getAccountInfo(@RequestBody AccountDTO account) {
        Account acc = accountService.findByCustomerId(account);
        List<MyStockDTO> mystock = myStockService.findAllMyStockByCustomerId(account.getCustomerId());

        long sumTotalNowPrice = mystock.stream()
                .mapToLong(s -> s.getNowPrice() * s.getAmount())
                .sum();

        if (acc == null) {
            return ResponseEntity.ok(new AccountDTO());
        }
        return ResponseEntity.ok(AccountDTO.builder()
                .id(acc.getId())
                .name(acc.getName())
                .deposit(acc.getDeposit())
                .seedMoney(acc.getSeedMoney())
                .sumTotalNowPrice(sumTotalNowPrice)
                .build());
    }

    @PutMapping("/info/account")
    public ResponseEntity<?> updateAccount(@RequestBody AccountDTO account) {
        try {
            accountService.updateAccount(account);
            return ResponseEntity.ok("성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/info/account")
    public ResponseEntity<?> deleteAccount(@RequestBody AccountDTO account) {
        try {
            accountService.deleteAccount(account);
            return ResponseEntity.ok("성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/account/reset")
    public ResponseEntity<?> resetAccount(@RequestBody AccountDTO account) {
        try {
            accountService.resetAccount(account.getCustomerId());
            return ResponseEntity.ok("성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
