package dev.pomyharry.stocksimulator.back.component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pomyharry.stocksimulator.back.model.dto.FuturesItemDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 선물/지수 시세 조회 컴포넌트.
 *
 * 데이터 소스:
 *  - KOSPI200 : KRX 데이터 포털
 *  - NASDAQ100, SP500 : 네이버 증권 API (api.stock.naver.com)
 *  - GOLD, OIL_WTI : Yahoo Finance v8 (백그라운드 스케줄러)
 *
 * 모든 데이터는 백그라운드에서 주기적으로 갱신되고, API 호출은 캐시만 반환합니다.
 */
@Slf4j
@Component
public class FuturesDataComponent {

    public static final List<FuturesItemDTO> CATALOG = List.of(
            FuturesItemDTO.builder()
                    .code("KOSPI200").name("코스피200 선물").category("INDEX")
                    .currency("KRW").priceUnit("pt").yahooSymbol("^KS200").build(),
            FuturesItemDTO.builder()
                    .code("NASDAQ100").name("나스닥100 선물").category("INDEX")
                    .currency("USD").priceUnit("pt").yahooSymbol("NQ=F").build(),
            FuturesItemDTO.builder()
                    .code("SP500").name("S&P500 선물").category("INDEX")
                    .currency("USD").priceUnit("pt").yahooSymbol("ES=F").build(),
            FuturesItemDTO.builder()
                    .code("GOLD").name("금 선물").category("COMMODITY")
                    .currency("USD").priceUnit("$/oz").yahooSymbol("GC=F").build(),
            FuturesItemDTO.builder()
                    .code("OIL_WTI").name("WTI원유 선물").category("COMMODITY")
                    .currency("USD").priceUnit("$/bbl").yahooSymbol("CL=F").build()
    );

    private static final Map<String, FuturesItemDTO> CATALOG_MAP = CATALOG.stream()
            .collect(Collectors.toMap(FuturesItemDTO::getCode, f -> f));

    // 네이버 API 매핑: 코드 → 네이버 index 코드
    // api.stock.naver.com/index/{code}/basic 또는 m.stock.naver.com/api/index/{code}/basic
    private static final Map<String, NaverSource> NAVER_MAP = Map.of(
            "NASDAQ100", new NaverSource("api.stock.naver.com", "/index/.NDX/basic", true),
            "SP500", new NaverSource("api.stock.naver.com", "/index/.INX/basic", true),
            "KOSPI200", new NaverSource("m.stock.naver.com", "/api/index/KPI200/basic", false)
    );

    // 금/원유는 Yahoo가 차단되었으므로 한국 ETF 가격을 프록시로 사용
    // KODEX 골드선물(H): 132030, TIGER 원유선물Enhanced(H): 130680
    private static final Map<String, String> ETF_PROXY_MAP = Map.of(
            "GOLD", "132030",
            "OIL_WTI", "130680"
    );

    private record NaverSource(String host, String path, boolean isIndex) {}

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    // 캐시: 백그라운드 갱신 기반 (TTL 없음 — 스케줄러가 주기적 갱신)
    private final ConcurrentHashMap<String, FuturesItemDTO> cache = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // 공개 API — 캐시만 반환 (블로킹 없음)
    // ----------------------------------------------------------------

    public List<FuturesItemDTO> getAllPrices() {
        return CATALOG.stream().map(f -> getPrice(f.getCode())).collect(Collectors.toList());
    }

    public FuturesItemDTO getPrice(String futuresCode) {
        FuturesItemDTO cached = cache.get(futuresCode);
        if (cached != null) return cached;

        // 캐시 미스 → 기본 템플릿 반환
        FuturesItemDTO template = CATALOG_MAP.get(futuresCode);
        if (template == null) return null;
        return FuturesItemDTO.builder()
                .code(template.getCode()).name(template.getName())
                .category(template.getCategory()).currency(template.getCurrency())
                .priceUnit(template.getPriceUnit()).yahooSymbol(template.getYahooSymbol())
                .available(false).build();
    }

    // ----------------------------------------------------------------
    // 앱 시작 시 즉시 갱신 + 주기적 백그라운드 갱신
    // ----------------------------------------------------------------

    @PostConstruct
    public void init() {
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            refreshAll();
        }, "futures-init").start();
    }

    /** 3분마다 갱신 */
    @Scheduled(fixedDelay = 180_000, initialDelay = 180_000)
    public void scheduledRefresh() {
        refreshAll();
    }

    private void refreshAll() {
        log.info("선물 시세 갱신 시작");

        // 1. 네이버 API (NASDAQ100, SP500, KOSPI200) — 빠르고 안정적
        for (Map.Entry<String, NaverSource> entry : NAVER_MAP.entrySet()) {
            fetchFromNaver(entry.getKey(), entry.getValue());
        }

        // 2. 금/원유 — 네이버 ETF 가격을 프록시로 사용 (Yahoo 429 차단 대응)
        for (Map.Entry<String, String> entry : ETF_PROXY_MAP.entrySet()) {
            fetchFromNaverEtfProxy(entry.getKey(), entry.getValue());
        }

        int ok = (int) cache.values().stream().filter(FuturesItemDTO::isAvailable).count();
        log.info("선물 시세 갱신 완료: {}/{}", ok, CATALOG.size());
    }

    // ----------------------------------------------------------------
    // 네이버 증권 API (api.stock.naver.com)
    // ----------------------------------------------------------------

    private void fetchFromNaver(String futuresCode, NaverSource source) {
        try {
            String urlStr = "https://" + source.host() + source.path();
            String body = httpGet(urlStr, Map.of(
                    "User-Agent", USER_AGENT,
                    "Accept", "application/json"
            ), 8000);

            if (body == null) return;

            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            String priceStr = getJsonStr(root, "closePrice");
            if (priceStr == null) return;

            double price = parseDouble(priceStr);
            double change = parseDouble(getJsonStr(root, "compareToPreviousClosePrice"));
            double changeRate = parseDouble(getJsonStr(root, "fluctuationsRatio"));

            if (price > 0) {
                FuturesItemDTO template = CATALOG_MAP.get(futuresCode);
                cache.put(futuresCode, FuturesItemDTO.builder()
                        .code(template.getCode()).name(template.getName())
                        .category(template.getCategory()).currency(template.getCurrency())
                        .priceUnit(template.getPriceUnit()).yahooSymbol(template.getYahooSymbol())
                        .currentPrice(price).change(change).changeRate(changeRate)
                        .available(true).build());
            }
        } catch (Exception e) {
            log.warn("네이버 지수 조회 실패 [{}]: {}", futuresCode, e.getMessage());
        }
    }

    /** 네이버 ETF 시세를 프록시로 사용 (금/원유 — Yahoo 차단 대응) */
    private void fetchFromNaverEtfProxy(String futuresCode, String etfCode) {
        try {
            String urlStr = "https://m.stock.naver.com/api/stock/" + etfCode + "/basic";
            String body = httpGet(urlStr, Map.of(
                    "User-Agent", USER_AGENT,
                    "Accept", "application/json"
            ), 7000);

            if (body == null) return;

            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            String priceStr = getJsonStr(root, "closePrice");
            if (priceStr == null) return;

            double price = parseDouble(priceStr);
            double change = parseDouble(getJsonStr(root, "compareToPreviousClosePrice"));
            double changeRate = parseDouble(getJsonStr(root, "fluctuationsRatio"));

            if (price > 0) {
                FuturesItemDTO template = CATALOG_MAP.get(futuresCode);
                cache.put(futuresCode, FuturesItemDTO.builder()
                        .code(template.getCode()).name(template.getName())
                        .category(template.getCategory()).currency(template.getCurrency())
                        .priceUnit("원(ETF)").yahooSymbol(template.getYahooSymbol())
                        .currentPrice(price).change(change).changeRate(changeRate)
                        .available(true).build());
            }
        } catch (Exception e) {
            log.warn("네이버 ETF 프록시 조회 실패 [{}→{}]: {}", futuresCode, etfCode, e.getMessage());
        }
    }

    // (KRX, Yahoo 제거 — 네이버 API + ETF 프록시로 완전 대체)

    // ----------------------------------------------------------------
    // HTTP 유틸
    // ----------------------------------------------------------------

    private String httpGet(String urlStr, Map<String, String> headers, int timeoutMs) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            headers.forEach(conn::setRequestProperty);

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("HTTP {} [{}]", status, urlStr);
                return null;
            }
            return readStream(conn.getInputStream());
        } catch (Exception e) {
            log.warn("연결 실패 [{}]: {}", urlStr, e.getMessage());
            return null;
        }
    }

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String getJsonStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        String s = obj.get(key).getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    private double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            return Double.parseDouble(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
