package dev.stocknest.controller;

import dev.stocknest.model.dto.CustomerDTO;
import dev.stocknest.model.dto.StockDTO;
import dev.stocknest.model.entity.WatchStock;
import dev.stocknest.service.StockDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
public class StockDataController {

    private final StockDataService stockDataService;

    @PostMapping("/stock-data")
    public ResponseEntity<?> getStockChart(@RequestBody CustomerDTO customer) {
        List<WatchStock> watchStocks = stockDataService.findAllStocks(customer);
        List<StockDTO> stockData = stockDataService.getStockChart(watchStocks);
        return ResponseEntity.ok(stockData);
    }
}
