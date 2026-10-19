package dev.stocknest.controller;

import dev.stocknest.component.EtfDataComponent;
import dev.stocknest.model.dto.EtfItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/etf")
@RequiredArgsConstructor
public class EtfController {

    private final EtfDataComponent etfDataComponent;

    /** 전체 ETF 목록 + 현재가 */
    @GetMapping
    public ResponseEntity<List<EtfItemDTO>> getAll() {
        return ResponseEntity.ok(etfDataComponent.getAllPrices());
    }

    /** 특정 ETF 현재가 */
    @GetMapping("/{code}")
    public ResponseEntity<?> getDetail(@PathVariable String code) {
        EtfItemDTO item = etfDataComponent.getPrice(code);
        if (item == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(item);
    }
}
