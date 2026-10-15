package dev.pomyharry.stocksimulator.back.controller;

import dev.pomyharry.stocksimulator.back.model.dto.StockDTO;
import dev.pomyharry.stocksimulator.back.service.KrxStockListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final KrxStockListService krxStockListService;

    @PostMapping
    public ResponseEntity<List<StockDTO>> searchStocks(@RequestBody StockDTO stockDTO) {
        String query = stockDTO.getName();
        if (query == null || query.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<StockDTO> results = krxStockListService.search(query);
        log.info("종목 검색 [{}] → {}개", query, results.size());
        return ResponseEntity.ok(results);
    }
}
