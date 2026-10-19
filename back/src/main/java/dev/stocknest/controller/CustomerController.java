package dev.stocknest.controller;

import dev.stocknest.model.dto.CustomerDTO;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.service.AccountService;
import dev.stocknest.service.CustomerService;
import dev.stocknest.service.WatchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;
    private final WatchStockService watchStockService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CustomerDTO customer) {
        try {
            return ResponseEntity.ok(customerService.login(customer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> createCustomer(@RequestBody CustomerDTO customer) {
        try {
            Customer c = customerService.create(Customer.builder()
                    .name(customer.getName())
                    .email(customer.getEmail())
                    .password(customer.getPassword())
                    .build());
            return ResponseEntity.ok(c);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/info/customer")
    public ResponseEntity<?> getCustomerInfo(@RequestBody CustomerDTO customer) {
        try {
            Customer c = customerService.findById(customer.getId());
            return ResponseEntity.ok(CustomerDTO.builder()
                    .name(c.getName())
                    .email(c.getEmail())
                    .password(c.getPassword())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/info/customer")
    public ResponseEntity<?> updateCustomerInfo(@RequestBody CustomerDTO customer) {
        try {
            customerService.updateCustomerInfo(customer);
            return ResponseEntity.ok("성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/info/customer")
    public ResponseEntity<?> deleteCustomerInfo(@RequestBody CustomerDTO customer) {
        try {
            Customer c = customerService.findById(customer.getId());
            accountService.deleteAllAccount(c);
            watchStockService.deleteAllWatchList(c);
            customerService.deleteCustomerInfO(customer);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
