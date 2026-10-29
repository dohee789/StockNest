package dev.stocknest.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stocknest.model.dto.CandleDTO;
import dev.stocknest.model.dto.StockDetailDTO;
import dev.stocknest.service.KisApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 한국투자증권 Open API로 캔들 OHLCV + 종목 상세 정보 조회.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandleDataComponent {

    private final KisApiClient kisApiClient;

    private static final DateTimeFormatter KIS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter OUTPUT_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<CandleDTO> getCandles(String stockCode) {
        return getCandles(stockCode, "day");
    }

    /**
     * KIS inquire-daily-itemchartprice API로 OHLCV 캔들 데이터 조회.
     * timeframe: "day" | "week" | "month"
     */
    public List<CandleDTO> getCandles(String stockCode, String timeframe) {
        String period;
        LocalDate startDate;
        LocalDate endDate = LocalDate.now();

        switch (timeframe) {
            case "week"  -> { period = "W"; startDate = endDate.minusYears(2); }
            case "month" -> { period = "M"; startDate = endDate.minusYears(5); }
            default      -> { period = "D"; startDate = endDate.minusMonths(6); }
        }

        try {
            List<CandleDTO> result = new ArrayList<>();
            String endStr = endDate.format(KIS_DATE_FMT);
            String startStr = startDate.format(KIS_DATE_FMT);
            int maxPages = 5; // prevent infinite loop

            for (int page = 0; page < maxPages; page++) {
                JsonArray output2 = kisApiClient.getDailyCandles(stockCode, startStr, endStr, period);
                if (output2 == null || output2.isEmpty()) break;

                for (JsonElement el : output2) {
                    if (!el.isJsonObject()) continue;
                    JsonObject item = el.getAsJsonObject();

                    String dateRaw = getStr(item, "stck_bsop_date");
                    long close = parseLong(item, "stck_clpr");
                    if (dateRaw == null || close == 0) continue;

                    String date = LocalDate.parse(dateRaw, KIS_DATE_FMT).format(OUTPUT_DATE_FMT);

                    result.add(CandleDTO.builder()
                            .date(date)
                            .open(parseLong(item, "stck_oprc"))
                            .high(parseLong(item, "stck_hgpr"))
                            .low(parseLong(item, "stck_lwpr"))
                            .close(close)
                            .volume(parseLong(item, "acml_vol"))
                            .build());
                }

                // If less than 100 records, no more pages
                if (output2.size() < 100) break;

                // Next page: set endDate to day before the oldest record in current batch
                JsonObject lastItem = output2.get(output2.size() - 1).getAsJsonObject();
                String lastDate = getStr(lastItem, "stck_bsop_date");
                if (lastDate == null) break;
                endStr = LocalDate.parse(lastDate, KIS_DATE_FMT).minusDays(1).format(KIS_DATE_FMT);

                // Stop if we've gone past the requested start date
                if (endStr.compareTo(startStr) < 0) break;
            }

            // Sort ascending by date (KIS returns newest first)
            result.sort(Comparator.comparing(CandleDTO::getDate));
            return result;

        } catch (Exception e) {
            log.warn("KIS candle fetch failed [{}]: {}", stockCode, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * KIS inquire-price API로 종목 상세 정보(거래량, 52주 고/저, PER/PBR, 종목명) 보완.
     * Yahoo에서는 2번 호출(v8 + v10)이 필요했지만, KIS는 1번 호출로 모두 제공.
     */
    public void enrichStockDetail(StockDetailDTO dto) {
        try {
            JsonObject response = kisApiClient.getCurrentPrice(dto.getCode());
            if (response == null || !response.has("output")) return;

            JsonObject data = response.getAsJsonObject("output");
            DecimalFormat df = new DecimalFormat("#,###");

            // Name
            String name = getStr(data, "hts_kor_isnm");
            if (name != null && (dto.getName() == null || dto.getName().isBlank())) {
                dto.setName(name);
            }

            // Volume
            long volume = parseLong(data, "acml_vol");
            if (volume > 0) {
                dto.setVolume(df.format(volume));
            }

            // 52-week high/low
            long high52w = parseLong(data, "w52_hgpr");
            long low52w = parseLong(data, "w52_lwpr");
            if (high52w > 0) dto.setHigh52w(df.format(high52w));
            if (low52w > 0) dto.setLow52w(df.format(low52w));

            // PER / PBR
            String per = getStr(data, "per");
            String pbr = getStr(data, "pbr");
            if (per != null && !"0.00".equals(per)) dto.setPer(per);
            if (pbr != null && !"0.00".equals(pbr)) dto.setPbr(pbr);

        } catch (Exception e) {
            log.warn("KIS stock detail enrich failed [{}]: {}", dto.getCode(), e.getMessage());
        }
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
