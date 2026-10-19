package dev.stocknest.controller;

import dev.stocknest.model.dto.MyStockDTO;
import dev.stocknest.service.MyStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class MyStockController {

    private final MyStockService myStockService;

    @PostMapping("/buy-stock")
    public ResponseEntity<?> buyStock(@RequestBody MyStockDTO myStockDTO) {
        try {
            myStockService.buyStock(myStockDTO);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/sell-stock")
    public ResponseEntity<?> sellStock(@RequestBody MyStockDTO myStockDTO) {
        try {
            myStockService.sellStock(myStockDTO);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/my-stock")
    public ResponseEntity<?> getMyStocks(@RequestBody MyStockDTO myStock) {
        try {
            List<MyStockDTO> myStocks = myStockService.findAllMyStockByCustomerId(myStock.getCustomerId());
            return ResponseEntity.ok(myStocks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
