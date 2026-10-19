package dev.stocknest.controller;

import dev.stocknest.exception.DuplicationException;
import dev.stocknest.model.dto.CustomerDTO;
import dev.stocknest.model.dto.ErrorResponseDTO;
import dev.stocknest.model.dto.StockDTO;
import dev.stocknest.model.dto.WatchStockDTO;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.model.entity.WatchStock;
import dev.stocknest.service.CustomerService;
import dev.stocknest.service.WatchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class WatchStockController {

    private final WatchStockService watchStockService;
    private final CustomerService customerService;

    @PostMapping("/watch")
    public ResponseEntity<?> createWatchList(@RequestBody WatchStockDTO watchStock) {
        try {
            Customer customer = customerService.findById(watchStock.getCustomerId());
            WatchStock ws = WatchStock.builder()
                    .stockCode(watchStock.getCode())
                    .stockName(watchStock.getName() != null ? watchStock.getName() : watchStock.getCode())
                    .customer(customer)
                    .build();
            watchStockService.createWatchList(ws);
            return ResponseEntity.ok("success");
        } catch (DuplicationException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO());
        }
    }

    @PostMapping("/watch-list")
    public ResponseEntity<?> findAllWatchStockByCustomerId(@RequestBody CustomerDTO customerDTO) {
        try {
            List<WatchStockDTO> watchList = watchStockService.findAllWatchStockByCustomerId(customerDTO.getId());
            return ResponseEntity.ok(watchList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/watch")
    public ResponseEntity<?> deleteByStockCode(@RequestBody StockDTO stock) {
        try {
            watchStockService.deleteByStockCode(stock.getCode());
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
