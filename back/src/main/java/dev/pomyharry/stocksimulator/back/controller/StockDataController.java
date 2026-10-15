package dev.pomyharry.stocksimulator.back.controller;

import dev.pomyharry.stocksimulator.back.model.dto.CustomerDTO;
import dev.pomyharry.stocksimulator.back.model.dto.StockDTO;
import dev.pomyharry.stocksimulator.back.model.entity.WatchStock;
import dev.pomyharry.stocksimulator.back.service.StockDataService;
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
