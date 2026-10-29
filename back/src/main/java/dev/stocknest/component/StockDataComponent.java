package dev.stocknest.component;

import com.google.gson.JsonObject;
import dev.stocknest.model.dto.StockDTO;
import dev.stocknest.service.KisApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;

/**
 * 한국투자증권 Open API로 현재가·등락 조회.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockDataComponent {

    private final KisApiClient kisApiClient;

    public StockDTO getStockChart(String stockCode, String stockName) {
        try {
            JsonObject output = kisApiClient.getCurrentPrice(stockCode);
            if (output == null || !output.has("output")) {
                return defaultDto(stockCode, stockName);
            }

            JsonObject data = output.getAsJsonObject("output");

            long price = parseLong(data, "stck_prpr");
            long change = parseLong(data, "prdy_vrss");
            String changeRate = getStr(data, "prdy_ctrt");
            String name = getStr(data, "hts_kor_isnm");

            DecimalFormat df = new DecimalFormat("#,###");
            String priceStr = df.format(price);
            String changeStr = (change >= 0 ? "▲ " : "▼ ") + df.format(Math.abs(change));
            String changeRateStr = (change >= 0 ? "+" : "") + changeRate + "%";

            return new StockDTO(
                    stockCode,
                    (stockName != null && !stockName.isBlank()) ? stockName : (name != null ? name : stockCode),
                    priceStr,
                    changeStr,
                    changeRateStr,
                    ""
            );

        } catch (Exception e) {
            log.warn("KIS stock price fetch failed [{}]: {}", stockCode, e.getMessage());
            return defaultDto(stockCode, stockName);
        }
    }

    private StockDTO defaultDto(String stockCode, String stockName) {
        return new StockDTO(stockCode, stockName != null ? stockName : stockCode, "-", "-", "-", "");
    }

    private long parseLong(JsonObject obj, String key) {
        try {
            String val = getStr(obj, key);
            return val != null ? Long.parseLong(val) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        String s = obj.get(key).getAsString().trim();
        return s.isEmpty() ? null : s;
    }
}
