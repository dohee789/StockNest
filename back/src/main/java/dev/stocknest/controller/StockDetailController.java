package dev.stocknest.controller;

import dev.stocknest.component.CandleDataComponent;
import dev.stocknest.component.StockDataComponent;
import dev.stocknest.model.dto.CandleDTO;
import dev.stocknest.model.dto.StockDTO;
import dev.stocknest.model.dto.StockDetailDTO;
import dev.stocknest.service.KrxStockListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
public class StockDetailController {

    private final StockDataComponent stockDataComponent;
    private final CandleDataComponent candleDataComponent;
    private final KrxStockListService krxStockListService;

    /**
     * GET /stock-detail/{code}
     * 종목 상세 정보 (현재가, 캔들 OHLCV, 추가 통계) 반환
     * DB stock 테이블 불필요 — Naver Finance에서 직접 조회
     */
    @GetMapping("/stock-detail/{code}")
    public ResponseEntity<?> getStockDetail(
            @PathVariable String code,
            @RequestParam(defaultValue = "day") String timeframe) {
        try {
            // 현재가 / 등락 (종목명은 임시로 code 사용, enrichStockDetail에서 덮어씀)
            StockDTO priceInfo = stockDataComponent.getStockChart(code, code);

            // 캔들 데이터
            List<CandleDTO> candles = candleDataComponent.getCandles(code, timeframe);

            StockDetailDTO detail = StockDetailDTO.builder()
                    .code(code)
                    .price(priceInfo != null ? priceInfo.getPrice() : "-")
                    .change(priceInfo != null ? priceInfo.getChange() : "-")
                    .changeRate(priceInfo != null ? priceInfo.getChangeRate() : "-")
                    .candles(candles)
                    .build();

            // 추가 정보 (종목명·거래량·52주 고저·PER/PBR)
            candleDataComponent.enrichStockDetail(detail);

            // KRX 한국어 종목명 우선 사용
            String krxName = krxStockListService.getNameByCode(code);
            if (krxName != null) {
                detail.setName(krxName);
            } else if (detail.getName() == null || detail.getName().isBlank()) {
                detail.setName(priceInfo != null && priceInfo.getName() != null
                        ? priceInfo.getName() : code);
            }

            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
