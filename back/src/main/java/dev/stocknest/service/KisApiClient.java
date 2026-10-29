package dev.stocknest.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stocknest.config.KisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 한국투자증권 Open API client.
 * Provides current price, candle OHLCV, and market index data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisApiClient {

    private final KisConfig kisConfig;
    private final KisTokenManager tokenManager;

    // ── Current Price ────────────────────────────────────────────────

    /**
     * 주식 현재가 조회 (inquire-price).
     * Returns raw KIS JSON "output" object, or null on failure.
     */
    public JsonObject getCurrentPrice(String stockCode) {
        String path = "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?FID_COND_MRKT_DIV_CODE=J"
                + "&FID_INPUT_ISCD=" + stockCode;

        return callApi(path, "FHKST01010100");
    }

    // ── Candle (Daily Chart Price) ───────────────────────────────────

    /**
     * 주식 일/주/월봉 조회 (inquire-daily-itemchartprice).
     * @param period "D" (day), "W" (week), "M" (month)
     * @param startDate yyyyMMdd
     * @param endDate   yyyyMMdd
     * Returns raw KIS JSON "output2" array, or empty array on failure.
     */
    public JsonArray getDailyCandles(String stockCode, String startDate, String endDate, String period) {
        String path = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"
                + "?FID_COND_MRKT_DIV_CODE=J"
                + "&FID_INPUT_ISCD=" + stockCode
                + "&FID_INPUT_DATE_1=" + startDate
                + "&FID_INPUT_DATE_2=" + endDate
                + "&FID_PERIOD_DIV_CODE=" + period
                + "&FID_ORG_ADJ_PRC=0";

        JsonObject result = callApi(path, "FHKST03010100");
        if (result != null && result.has("output2")) {
            return result.getAsJsonArray("output2");
        }
        return new JsonArray();
    }

    // ── Market Index ─────────────────────────────────────────────────

    /**
     * 업종(지수) 현재가 조회 (inquire-index-price).
     * @param indexCode "0001" (KOSPI), "1001" (KOSDAQ)
     * Returns raw KIS JSON "output" object, or null on failure.
     */
    public JsonObject getMarketIndex(String indexCode) {
        String path = "/uapi/domestic-stock/v1/quotations/inquire-index-price"
                + "?FID_COND_MRKT_DIV_CODE=U"
                + "&FID_INPUT_ISCD=" + indexCode;

        return callApi(path, "FHPUP02100000");
    }

    // ── Internal ─────────────────────────────────────────────────────

    private JsonObject callApi(String path, String trId) {
        try {
            Thread.sleep(50); // rate limit: max 20 req/sec

            String token = tokenManager.getToken();
            if (token == null) {
                log.warn("KIS API call skipped — no token available");
                return null;
            }

            String urlStr = kisConfig.getBaseUrl() + path;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("authorization", "Bearer " + token);
            conn.setRequestProperty("appkey", kisConfig.getAppKey());
            conn.setRequestProperty("appsecret", kisConfig.getAppSecret());
            conn.setRequestProperty("tr_id", trId);

            int status = conn.getResponseCode();
            String response = readStream(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream());

            if (status != 200) {
                log.warn("KIS API error ({}): path={}, response={}", status, path, response);

                // Token expired — refresh and retry once
                if (status == 401) {
                    log.info("KIS token expired, refreshing and retrying...");
                    tokenManager.getToken(); // forces refresh
                    return retryOnce(path, trId);
                }
                return null;
            }

            JsonObject root = JsonParser.parseString(response).getAsJsonObject();

            // Check KIS-level error
            if (root.has("rt_cd") && !"0".equals(root.get("rt_cd").getAsString())) {
                String msg = root.has("msg1") ? root.get("msg1").getAsString() : "unknown";
                log.warn("KIS API business error: rt_cd={}, msg={}", root.get("rt_cd").getAsString(), msg);
                return null;
            }

            // Return the full response — callers extract "output" or "output2"
            return root;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("KIS API call failed: path={}, error={}", path, e.getMessage());
            return null;
        }
    }

    private JsonObject retryOnce(String path, String trId) {
        try {
            Thread.sleep(100);
            String token = tokenManager.getToken();
            if (token == null) return null;

            String urlStr = kisConfig.getBaseUrl() + path;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("authorization", "Bearer " + token);
            conn.setRequestProperty("appkey", kisConfig.getAppKey());
            conn.setRequestProperty("appsecret", kisConfig.getAppSecret());
            conn.setRequestProperty("tr_id", trId);

            int status = conn.getResponseCode();
            String response = readStream(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream());

            if (status != 200) {
                log.error("KIS API retry failed ({}): {}", status, response);
                return null;
            }

            return JsonParser.parseString(response).getAsJsonObject();

        } catch (Exception e) {
            log.error("KIS API retry exception: {}", e.getMessage());
            return null;
        }
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
