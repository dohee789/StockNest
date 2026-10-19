package dev.stocknest.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stocknest.model.dto.StockDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

@Slf4j
@Component
public class StockDataComponent {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    /**
     * Yahoo Finance v8 chart API로 현재가·등락 조회.
     * 한국 주식: {code}.KS (KOSPI) → {code}.KQ (KOSDAQ) 순서로 시도.
     */
    public StockDTO getStockChart(String stockCode, String stockName) {
        StockDTO dto = fetchFromYahoo(stockCode + ".KS", stockCode, stockName);
        if (dto != null) return dto;
        dto = fetchFromYahoo(stockCode + ".KQ", stockCode, stockName);
        if (dto != null) return dto;
        // Yahoo 조회 실패 시 기본값 반환 (null 방지)
        return new StockDTO(stockCode, stockName != null ? stockName : stockCode, "-", "-", "-", "");
    }

    private StockDTO fetchFromYahoo(String symbol, String stockCode, String stockName) {
        try {
            String encoded = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            String urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/"
                    + encoded + "?interval=1d&range=1d";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");

            if (conn.getResponseCode() != 200) return null;

            JsonObject root = JsonParser.parseString(readStream(conn.getInputStream())).getAsJsonObject();
            JsonArray resultArr = root.getAsJsonObject("chart").getAsJsonArray("result");
            if (resultArr == null || resultArr.isEmpty()) return null;

            JsonObject meta = resultArr.get(0).getAsJsonObject().getAsJsonObject("meta");
            double price = meta.get("regularMarketPrice").getAsDouble();
            double prevClose = meta.has("previousClose") ? meta.get("previousClose").getAsDouble()
                    : meta.has("chartPreviousClose") ? meta.get("chartPreviousClose").getAsDouble()
                    : price;
            double changeVal = price - prevClose;
            double changeRate = prevClose != 0 ? changeVal / prevClose * 100 : 0;

            String name = stockName;
            if (name == null || name.isBlank()) {
                name = getStr(meta, "shortName");
                if (name == null) name = getStr(meta, "longName");
                if (name == null) name = stockCode;
            }

            DecimalFormat df = new DecimalFormat("#,###");
            String priceStr = df.format(Math.round(price));
            String changeStr = (changeVal >= 0 ? "▲ " : "▼ ") + df.format(Math.abs(Math.round(changeVal)));
            String changeRateStr = String.format("%+.2f%%", changeRate);

            return new StockDTO(stockCode, name, priceStr, changeStr, changeRateStr, "");

        } catch (Exception e) {
            log.debug("Yahoo 주식 조회 실패 [{}]: {}", symbol, e.getMessage());
            return null;
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
}
