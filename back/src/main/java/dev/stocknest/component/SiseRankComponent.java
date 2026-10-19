package dev.stocknest.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stocknest.model.dto.SiseRankDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 네이버 증권 모바일 API를 활용한 급등/급락 종목 랭킹 조회.
 * API: https://m.stock.naver.com/api/stocks/{up|down}
 */
@Slf4j
@Component
public class SiseRankComponent {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final int LIMIT = 20;

    private static final String RISING_URL  = "https://m.stock.naver.com/api/stocks/up?page=1&pageSize=" + LIMIT;
    private static final String FALLING_URL = "https://m.stock.naver.com/api/stocks/down?page=1&pageSize=" + LIMIT;

    /** 급등 top 20 */
    public List<SiseRankDTO> getRising() {
        return fetchFromNaverApi(RISING_URL);
    }

    /** 급락 top 20 */
    public List<SiseRankDTO> getFalling() {
        return fetchFromNaverApi(FALLING_URL);
    }

    /** 거래량 top 20 — 급등 데이터로 대체 (별도 API 없음) */
    public List<SiseRankDTO> getVolume() {
        return fetchFromNaverApi(RISING_URL);
    }

    /**
     * 네이버 증권 모바일 JSON API 호출.
     * 응답 구조: { "stocks": [ { itemCode, stockName, closePrice, ... }, ... ] }
     */
    private List<SiseRankDTO> fetchFromNaverApi(String apiUrl) {
        List<SiseRankDTO> result = new ArrayList<>();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                log.warn("네이버 API 응답 오류: {} (URL: {})", conn.getResponseCode(), apiUrl);
                return result;
            }

            String body = readStream(conn.getInputStream());
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray stocks = root.getAsJsonArray("stocks");
            if (stocks == null) return result;

            for (JsonElement el : stocks) {
                JsonObject item = el.getAsJsonObject();

                String code = getStr(item, "itemCode");
                String name = getStr(item, "stockName");
                if (code == null || name == null) continue;

                String price = getStr(item, "closePrice");
                if (price == null) price = "0";

                // 등락 방향
                String direction = "";
                if (item.has("compareToPreviousPrice") && !item.get("compareToPreviousPrice").isJsonNull()) {
                    JsonObject cmp = item.getAsJsonObject("compareToPreviousPrice");
                    String dirName = getStr(cmp, "name");
                    if ("RISING".equals(dirName) || "UPPER_LIMIT".equals(dirName)) {
                        direction = "▲ ";
                    } else if ("FALLING".equals(dirName) || "LOWER_LIMIT".equals(dirName)) {
                        direction = "▼ ";
                    }
                }

                // 전일비
                String changeRaw = getStr(item, "compareToPreviousClosePrice");
                String changeAbs = (changeRaw != null) ? changeRaw.replace("-", "") : "0";
                String change = direction + changeAbs;

                // 등락률
                String ratioRaw = getStr(item, "fluctuationsRatio");
                String changeRate;
                if (ratioRaw != null) {
                    double ratio = Double.parseDouble(ratioRaw);
                    changeRate = String.format("%+.2f%%", ratio);
                } else {
                    changeRate = "0.00%";
                }

                // 거래량
                String volume = getStr(item, "accumulatedTradingVolume");
                if (volume == null) volume = "0";

                result.add(SiseRankDTO.builder()
                        .code(code)
                        .name(name)
                        .price(price)
                        .change(change)
                        .changeRate(changeRate)
                        .volume(volume)
                        .build());

                if (result.size() >= LIMIT) break;
            }
        } catch (Exception e) {
            log.warn("네이버 API 조회 실패 ({}): {}", apiUrl, e.getMessage());
        }
        return result;
    }

    private String getStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        String s = obj.get(key).getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    private String readStream(InputStream is) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
