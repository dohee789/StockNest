package dev.stocknest.component;

import com.google.gson.JsonObject;
import dev.stocknest.model.dto.MarketIndexDTO;
import dev.stocknest.service.KisApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 한국투자증권 Open API로 KOSPI/KOSDAQ 지수 조회.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketIndexComponent {

    private final KisApiClient kisApiClient;

    public List<MarketIndexDTO> getMarketIndexes() {
        List<MarketIndexDTO> result = new ArrayList<>();

        MarketIndexDTO kospi = fetchIndex("KOSPI", "0001");
        result.add(kospi != null ? kospi : new MarketIndexDTO("KOSPI", "-", "-", "-", false));

        MarketIndexDTO kosdaq = fetchIndex("KOSDAQ", "1001");
        result.add(kosdaq != null ? kosdaq : new MarketIndexDTO("KOSDAQ", "-", "-", "-", false));

        return result;
    }

    private MarketIndexDTO fetchIndex(String name, String indexCode) {
        try {
            JsonObject response = kisApiClient.getMarketIndex(indexCode);
            if (response == null || !response.has("output")) return null;

            JsonObject data = response.getAsJsonObject("output");

            double value = parseDouble(data, "bstp_nmix_prpr");
            double change = parseDouble(data, "bstp_nmix_prdy_vrss");
            String changeRateStr = getStr(data, "bstp_nmix_prdy_ctrt");

            DecimalFormat dfVal = new DecimalFormat("#,##0.00");
            DecimalFormat dfChg = new DecimalFormat("0.00");

            String valueStr = dfVal.format(value);
            String changeStr = (change >= 0 ? "+" : "") + dfChg.format(change);
            String rateStr = (change >= 0 ? "+" : "") + changeRateStr + "%";

            return new MarketIndexDTO(name, valueStr, changeStr, rateStr, change >= 0);

        } catch (Exception e) {
            log.warn("KIS index fetch failed [{}]: {}", name, e.getMessage());
            return null;
        }
    }

    private double parseDouble(JsonObject obj, String key) {
        try {
            String val = getStr(obj, key);
            return val != null ? Double.parseDouble(val) : 0;
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
