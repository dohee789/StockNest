package dev.pomyharry.stocksimulator.back.controller;

import dev.pomyharry.stocksimulator.back.model.dto.FuturesItemDTO;
import dev.pomyharry.stocksimulator.back.model.dto.FuturesOrderDTO;
import dev.pomyharry.stocksimulator.back.model.dto.FuturesPositionDTO;
import dev.pomyharry.stocksimulator.back.service.FuturesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/futures")
@RequiredArgsConstructor
public class FuturesController {

    private final FuturesService futuresService;

    /** 전체 선물 목록 + 현재가 */
    @GetMapping
    public ResponseEntity<List<FuturesItemDTO>> getAll() {
        return ResponseEntity.ok(futuresService.getAvailableFutures());
    }

    /** 특정 선물 현재가 */
    @GetMapping("/{code}")
    public ResponseEntity<?> getDetail(@PathVariable String code) {
        FuturesItemDTO item = futuresService.getFuturesDetail(code);
        if (item == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(item);
    }

    /** 포지션 오픈 (롱/숏 진입) */
    @PostMapping("/open")
    public ResponseEntity<?> openPosition(@RequestBody FuturesOrderDTO dto) {
        try {
            futuresService.openPosition(dto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 포지션 청산 */
    @PostMapping("/close")
    public ResponseEntity<?> closePosition(@RequestBody Map<String, String> body) {
        try {
            futuresService.closePosition(body.get("positionId"), body.get("customerId"));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 고객의 전체 포지션 (OPEN + CLOSED) */
    @GetMapping("/positions/{customerId}")
    public ResponseEntity<List<FuturesPositionDTO>> getPositions(@PathVariable String customerId) {
        return ResponseEntity.ok(futuresService.getPositions(customerId));
    }
}
