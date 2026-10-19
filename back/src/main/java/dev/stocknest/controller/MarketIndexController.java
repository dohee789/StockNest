package dev.stocknest.controller;

import dev.stocknest.component.MarketIndexComponent;
import dev.stocknest.model.dto.MarketIndexDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/market-index")
public class MarketIndexController {

    private final MarketIndexComponent marketIndexComponent;

    public MarketIndexController(MarketIndexComponent marketIndexComponent) {
        this.marketIndexComponent = marketIndexComponent;
    }

    @GetMapping
    public ResponseEntity<?> getMarketIndexes() {
        try {
            List<MarketIndexDTO> indexes = marketIndexComponent.getMarketIndexes();
            return ResponseEntity.ok().body(indexes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
