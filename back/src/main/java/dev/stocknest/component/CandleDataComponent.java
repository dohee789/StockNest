package dev.stocknest.component;

import com.google.gson.*;
import dev.stocknest.model.dto.CandleDTO;
import dev.stocknest.model.dto.StockDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CandleDataComponent {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<CandleDTO> getCandles(String stockCode) {
        return getCandles(stockCode, "day");
    }

    /**
     * Yahoo Finance v8 chart API로 OHLCV 캔들 데이터 조회.
     * timeframe: "day" | "week" | "month"
     */
    public List<CandleDTO> getCandles(String stockCode, String timeframe) {
        String interval;
        String range;
        switch (timeframe) {
            case "week"  -> { interval = "1wk"; range = "2y";  }
            case "month" -> { interval = "1mo"; range = "5y";  }
            default      -> { interval = "1d";  range = "6mo"; }
        }

        List<CandleDTO> result = fetchCandles(stockCode + ".KS", interval, range);
        if (result.isEmpty()) {
            result = fetchCandles(stockCode + ".KQ", interval, range);
        }
        return result;
    }

    private List<CandleDTO> fetchCandles(String symbol, String interval, String range) {
        List<CandleDTO> result = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            String urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/"
                    + encoded + "?interval=" + interval + "&range=" + range;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) return result;

            JsonObject root = JsonParser.parseString(readStream(conn.getInputStream())).getAsJsonObject();
            JsonArray resultArr = root.getAsJsonObject("chart").getAsJsonArray("result");
            if (resultArr == null || resultArr.isEmpty()) return result;

            JsonObject data = resultArr.get(0).getAsJsonObject();
            JsonArray timestamps = data.getAsJsonArray("timestamp");
            JsonObject quote = data.getAsJsonObject("indicators")
                    .getAsJsonArray("quote").get(0).getAsJsonObject();

            JsonArray opens   = quote.getAsJsonArray("open");
            JsonArray highs   = quote.getAsJsonArray("high");
            JsonArray lows    = quote.getAsJsonArray("low");
            JsonArray closes  = quote.getAsJsonArray("close");
            JsonArray volumes = quote.getAsJsonArray("volume");

            for (int i = 0; i < timestamps.size(); i++) {
                if (closes.get(i).isJsonNull()) continue;

                long ts = timestamps.get(i).getAsLong();
                String date = Instant.ofEpochSecond(ts).atZone(KST).toLocalDate().format(DATE_FMT);

                result.add(CandleDTO.builder()
                        .date(date)
                        .open(toLong(opens, i))
                        .high(toLong(highs, i))
                        .low(toLong(lows, i))
                        .close(toLong(closes, i))
                        .volume(toLong(volumes, i))
                        .build());
            }
        } catch (Exception e) {
            log.debug("Yahoo 캔들 조회 실패 [{}]: {}", symbol, e.getMessage());
        }
        return result;
    }

    /**
     * Yahoo Finance meta 데이터로 종목 상세 정보(거래량, 52주 고/저, 종목명) 보완.
     * PER·PBR은 Yahoo v10 quoteSummary에서 추가 시도.
     */
    public void enrichStockDetail(StockDetailDTO dto) {
        boolean ok = enrichFromYahoo(dto, dto.getCode() + ".KS");
        if (!ok) enrichFromYahoo(dto, dto.getCode() + ".KQ");
    }

    private boolean enrichFromYahoo(StockDetailDTO dto, String symbol) {
        try {
            String encoded = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            String urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/"
                    + encoded + "?interval=1d&range=1y";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) return false;

            JsonObject root = JsonParser.parseString(readStream(conn.getInputStream())).getAsJsonObject();
            JsonArray resultArr = root.getAsJsonObject("chart").getAsJsonArray("result");
            if (resultArr == null || resultArr.isEmpty()) return false;

            JsonObject meta = resultArr.get(0).getAsJsonObject().getAsJsonObject("meta");

            // 종목명
            if (dto.getName() == null || dto.getName().isBlank()) {
                String name = getStr(meta, "shortName");
                if (name == null) name = getStr(meta, "longName");
                if (name != null) dto.setName(name);
            }

            // 거래량
            if (meta.has("regularMarketVolume") && !meta.get("regularMarketVolume").isJsonNull()) {
                DecimalFormat df = new DecimalFormat("#,###");
                dto.setVolume(df.format(meta.get("regularMarketVolume").getAsLong()));
            }

            // 52주 최고/최저
            DecimalFormat df = new DecimalFormat("#,###");
            if (meta.has("fiftyTwoWeekHigh") && !meta.get("fiftyTwoWeekHigh").isJsonNull()) {
                dto.setHigh52w(df.format(Math.round(meta.get("fiftyTwoWeekHigh").getAsDouble())));
            }
            if (meta.has("fiftyTwoWeekLow") && !meta.get("fiftyTwoWeekLow").isJsonNull()) {
                dto.setLow52w(df.format(Math.round(meta.get("fiftyTwoWeekLow").getAsDouble())));
            }

            // PER / PBR (v10 quoteSummary)
            fetchPERPBR(dto, encoded);
            return true;

        } catch (Exception e) {
            log.debug("Yahoo 종목상세 조회 실패 [{}]: {}", symbol, e.getMessage());
            return false;
        }
    }

    private void fetchPERPBR(StockDetailDTO dto, String encodedSymbol) {
        try {
            String urlStr = "https://query1.finance.yahoo.com/v10/finance/quoteSummary/"
                    + encodedSymbol + "?modules=summaryDetail";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) return;

            JsonObject root = JsonParser.parseString(readStream(conn.getInputStream())).getAsJsonObject();
            JsonObject sd = root.getAsJsonObject("quoteSummary")
                    .getAsJsonArray("result").get(0)
                    .getAsJsonObject().getAsJsonObject("summaryDetail");

            if (sd.has("trailingPE") && sd.get("trailingPE").isJsonObject()) {
                JsonElement fmt = sd.getAsJsonObject("trailingPE").get("fmt");
                if (fmt != null && !fmt.isJsonNull()) dto.setPer(fmt.getAsString());
            }
            if (sd.has("priceToBook") && sd.get("priceToBook").isJsonObject()) {
                JsonElement fmt = sd.getAsJsonObject("priceToBook").get("fmt");
                if (fmt != null && !fmt.isJsonNull()) dto.setPbr(fmt.getAsString());
            }
        } catch (Exception e) {
            log.debug("Yahoo PER/PBR 조회 실패: {}", e.getMessage());
        }
    }

    private long toLong(JsonArray arr, int i) {
        if (arr == null || i >= arr.size() || arr.get(i).isJsonNull()) return 0;
        try {
            return arr.get(i).getAsLong();
        } catch (Exception e) {
            return Math.round(arr.get(i).getAsDouble());
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
