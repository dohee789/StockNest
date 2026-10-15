package dev.pomyharry.stocksimulator.back.component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pomyharry.stocksimulator.back.model.dto.EtfItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 네이버 증권 모바일 API를 활용한 한국 ETF 시세 조회.
 * API: https://m.stock.naver.com/api/stock/{code}/basic
 */
@Slf4j
@Component
public class EtfDataComponent {

    public static final List<EtfItemDTO> CATALOG = List.of(
            etf("069500", "KODEX 200", "DOMESTIC_INDEX", "KOSPI 200"),
            etf("102110", "TIGER 200", "DOMESTIC_INDEX", "KOSPI 200"),
            etf("229200", "KODEX 코스닥150", "DOMESTIC_INDEX", "KOSDAQ 150"),
            etf("278540", "KODEX MSCI Korea", "DOMESTIC_INDEX", "MSCI Korea"),

            etf("133690", "TIGER 미국나스닥100", "FOREIGN_INDEX", "NASDAQ 100"),
            etf("360750", "TIGER 미국S&P500", "FOREIGN_INDEX", "S&P 500"),
            etf("241180", "TIGER 일본니케이225", "FOREIGN_INDEX", "Nikkei 225"),
            etf("099140", "KODEX 차이나H", "FOREIGN_INDEX", "Hang Seng China"),
            etf("195930", "TIGER 유로스탁스50", "FOREIGN_INDEX", "Euro Stoxx 50"),

            etf("091160", "KODEX 반도체", "SECTOR", "KRX 반도체"),
            etf("091170", "KODEX 자동차", "SECTOR", "KRX 자동차"),
            etf("244580", "KODEX 바이오", "SECTOR", "KRX 바이오"),
            etf("305540", "TIGER 2차전지테마", "SECTOR", "2차전지"),
            etf("139260", "TIGER 200 IT", "SECTOR", "KOSPI 200 IT"),
            etf("266360", "KODEX 헬스케어", "SECTOR", "KRX 헬스케어"),

            etf("132030", "KODEX 골드선물(H)", "COMMODITY", "금 선물"),
            etf("130680", "TIGER 원유선물Enhanced(H)", "COMMODITY", "WTI 원유"),
            etf("144600", "KODEX 은선물(H)", "COMMODITY", "은 선물"),

            etf("148070", "KODEX 국고채10년", "BOND", "국고채 10년"),
            etf("114260", "KODEX 국고채3년", "BOND", "국고채 3년"),
            etf("157450", "TIGER 단기통안채", "BOND", "통안채 1년"),

            etf("122630", "KODEX 레버리지", "LEVERAGE", "KOSPI 200 ×2"),
            etf("114800", "KODEX 인버스", "LEVERAGE", "KOSPI 200 ×(-1)"),
            etf("252670", "KODEX 200선물인버스2X", "LEVERAGE", "KOSPI 200 ×(-2)"),
            etf("233740", "KODEX 코스닥150레버리지", "LEVERAGE", "KOSDAQ 150 ×2")
    );

    private static final Map<String, EtfItemDTO> CATALOG_MAP = CATALOG.stream()
            .collect(Collectors.toMap(EtfItemDTO::getCode, e -> e));

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    // 캐시: 30초간 유효
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30_000;

    public List<EtfItemDTO> getAllPrices() {
        List<EtfItemDTO> results = new ArrayList<>();
        for (EtfItemDTO e : CATALOG) {
            results.add(getPrice(e.getCode()));
        }
        return results;
    }

    public EtfItemDTO getPrice(String etfCode) {
        EtfItemDTO template = CATALOG_MAP.get(etfCode);
        if (template == null) return null;

        // 캐시 확인
        CacheEntry cached = cache.get(etfCode);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        EtfItemDTO result = EtfItemDTO.builder()
                .code(template.getCode())
                .name(template.getName())
                .category(template.getCategory())
                .trackingIndex(template.getTrackingIndex())
                .available(false)
                .build();

        fetchFromNaver(result, etfCode);

        cache.put(etfCode, new CacheEntry(result));
        return result;
    }

    private void fetchFromNaver(EtfItemDTO result, String code) {
        try {
            String apiUrl = "https://m.stock.naver.com/api/stock/" + code + "/basic";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("네이버 ETF HTTP {} [{}]", status, code);
                return;
            }

            String body = readStream(conn.getInputStream());
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            String priceStr = getStr(root, "closePrice");
            if (priceStr == null) return;

            double price = Double.parseDouble(priceStr.replace(",", ""));

            String changeStr = getStr(root, "compareToPreviousClosePrice");
            double change = 0;
            if (changeStr != null) {
                change = Double.parseDouble(changeStr.replace(",", ""));
            }

            String ratioStr = getStr(root, "fluctuationsRatio");
            double changeRate = 0;
            if (ratioStr != null) {
                changeRate = Double.parseDouble(ratioStr);
            }

            result.setCurrentPrice(price);
            result.setChange(change);
            result.setChangeRate(changeRate);
            result.setAvailable(true);

        } catch (Exception e) {
            log.warn("네이버 ETF 조회 실패 [{}]: {}", code, e.getMessage());
        }
    }

    private String getStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        String s = obj.get(key).getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static EtfItemDTO etf(String code, String name, String category, String tracking) {
        return EtfItemDTO.builder()
                .code(code).name(name).category(category)
                .trackingIndex(tracking).available(false)
                .build();
    }

    private static class CacheEntry {
        final EtfItemDTO data;
        final long timestamp;

        CacheEntry(EtfItemDTO data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
