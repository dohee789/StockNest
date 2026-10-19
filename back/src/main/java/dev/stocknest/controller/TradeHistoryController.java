package dev.stocknest.controller;

import dev.stocknest.model.dto.TradeHistoryDTO;
import dev.stocknest.service.TradeHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trade-history")
public class TradeHistoryController {

    private final TradeHistoryService tradeHistoryService;

    public TradeHistoryController(TradeHistoryService tradeHistoryService) {
        this.tradeHistoryService = tradeHistoryService;
    }

    @PostMapping
    public ResponseEntity<?> getTradeHistory(@RequestBody Map<String, String> body) {
        try {
            String customerId = body.get("customerId");
            List<TradeHistoryDTO> histories = tradeHistoryService.findByCustomerId(customerId);
            return ResponseEntity.ok().body(histories);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
