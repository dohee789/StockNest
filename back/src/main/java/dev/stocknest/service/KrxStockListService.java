package dev.stocknest.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stocknest.model.dto.StockDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KrxStockListService {

    private volatile List<StockDTO> stocks = new ArrayList<>(FALLBACK);

    @PostConstruct
    public void loadStocks() {
        new Thread(() -> {
            try {
                List<StockDTO> loaded = fetchFromKrx();
                if (!loaded.isEmpty()) {
                    stocks = loaded;
                    log.info("KRX 주식 목록 로드 완료: {}개", stocks.size());
                } else {
                    log.warn("KRX 응답이 비어있어 기본 목록({})을 사용합니다", FALLBACK.size());
                }
            } catch (Exception e) {
                log.warn("KRX 주식 목록 로드 실패(기본 목록 사용): {}", e.getMessage());
            }
        }, "krx-stock-loader").start();
    }

    private List<StockDTO> fetchFromKrx() throws Exception {
        URL url = new URL("https://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("Referer",
                "https://data.krx.co.kr/contents/MDC/MDI/mdiLoader/index.cmd?menuId=MDC0201020101");
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        String body = "bld=dbms/MDC/STAT/standard/MDCSTAT01901&locale=ko_KR&mktId=ALL&share=1&money=1&csvxls_isNo=false";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        log.info("KRX API status: {}", status);

        if (status != 200) return List.of();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        String responseBody = sb.toString();
        log.info("KRX response preview: {}",
                responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody);

        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        if (!root.has("OutBlock_1")) return List.of();

        JsonArray items = root.getAsJsonArray("OutBlock_1");
        List<StockDTO> result = new ArrayList<>();

        for (JsonElement el : items) {
            if (!el.isJsonObject()) continue;
            JsonObject item = el.getAsJsonObject();

            String code = getString(item, "ISU_SRT_CD");
            String name = getString(item, "ISU_ABBRV");
            String secGrp = getString(item, "SECUGRP_NM");

            if (code == null || code.length() != 6 || name == null || name.isBlank()) continue;
            if (!"주권".equals(secGrp)) continue; // ETF, 수익증권 등 제외

            result.add(StockDTO.builder().code(code).name(name).build());
        }
        return result;
    }

    private String getString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString().trim();
    }

    public List<StockDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        String kw = keyword.trim();
        return stocks.stream()
                .filter(s -> s.getName().contains(kw) || s.getCode().startsWith(kw))
                .limit(8)
                .collect(Collectors.toList());
    }

    // ── 애플리케이션 기동 시 KRX 로드 실패해도 기본 동작 보장용 목록 ──────────
    private static final List<StockDTO> FALLBACK = List.of(
            s("005930", "삼성전자"), s("000660", "SK하이닉스"), s("005380", "현대차"),
            s("035420", "NAVER"), s("000270", "기아"), s("051910", "LG화학"),
            s("006400", "삼성SDI"), s("035720", "카카오"), s("068270", "셀트리온"),
            s("105560", "KB금융"), s("055550", "신한지주"), s("086790", "하나금융지주"),
            s("316140", "우리금융지주"), s("096770", "SK이노베이션"), s("011170", "롯데케미칼"),
            s("012330", "현대모비스"), s("018260", "삼성에스디에스"), s("010130", "고려아연"),
            s("003490", "대한항공"), s("036570", "엔씨소프트"), s("017670", "SK텔레콤"),
            s("030200", "KT"), s("032640", "LG유플러스"), s("000810", "삼성화재"),
            s("028260", "삼성물산"), s("009150", "삼성전기"), s("066570", "LG전자"),
            s("003550", "LG"), s("015760", "한국전력"), s("034020", "두산에너빌리티"),
            s("010950", "에스오일"), s("003670", "포스코퓨처엠"), s("005490", "POSCO홀딩스"),
            s("011780", "금호석유화학"), s("000720", "현대건설"), s("004170", "신세계"),
            s("023530", "롯데쇼핑"), s("139480", "이마트"), s("097950", "CJ제일제당"),
            s("024110", "기업은행"), s("086280", "현대글로비스"), s("267250", "HD현대"),
            s("329180", "HD현대중공업"), s("042660", "한화오션"), s("034730", "SK"),
            s("000100", "유한양행"), s("207940", "삼성바이오로직스"), s("068760", "셀트리온헬스케어"),
            s("001040", "CJ"), s("000080", "하이트진로"), s("011790", "SKC"),
            s("016360", "삼성증권"), s("021240", "코웨이"), s("028050", "삼성엔지니어링"),
            s("032830", "삼성생명"), s("036460", "한국가스공사"), s("039490", "키움증권"),
            s("042670", "두산밥캣"), s("047810", "한국항공우주"), s("064350", "현대로템"),
            s("066970", "엘앤에프"), s("069960", "현대백화점"), s("071050", "한국금융지주"),
            s("078930", "GS"), s("090430", "아모레퍼시픽"), s("010140", "삼성중공업"),
            s("006360", "GS건설"), s("002380", "KCC"), s("008770", "호텔신라"),
            s("009540", "HD한국조선해양"), s("004020", "현대제철"), s("009830", "한화솔루션"),
            s("010620", "HD현대미포"), s("011200", "HMM"), s("033780", "KT&G"),
            s("029780", "삼성카드"), s("030000", "제일기획"), s("000120", "CJ대한통운"),
            s("002790", "아모레G"), s("007070", "GS리테일"), s("007310", "오뚜기"),
            s("010060", "OCI홀딩스"), s("004800", "효성"), s("004990", "롯데지주"),
            s("020150", "롯데에너지머티리얼즈"), s("001800", "오리온홀딩스"), s("001680", "대상"),
            // KOSDAQ 주요 종목
            s("293490", "카카오게임즈"), s("247540", "에코프로비엠"), s("086520", "에코프로"),
            s("091990", "셀트리온헬스케어"), s("196170", "알테오젠"), s("263750", "펄어비스"),
            s("041510", "에스엠"), s("035900", "JYP Ent."), s("122870", "와이지엔터테인먼트"),
            s("112040", "위메이드"), s("067160", "아프리카TV"), s("357780", "솔브레인"),
            s("145020", "휴젤"), s("032500", "케이엠더블유"), s("095340", "ISC"),
            s("058470", "리노공업"), s("036930", "주성엔지니어링"), s("108320", "LX세미콘"),
            s("079550", "LIG넥스원"), s("065350", "신성이엔지"), s("383220", "F&F"),
            s("060260", "에스에프에이"), s("039030", "이오테크닉스"), s("095660", "네오위즈"),
            s("214150", "클래시스"), s("297090", "HLB생명과학"), s("028300", "HLB"),
            s("900140", "엘브이엠씨홀딩스"), s("241560", "두산로보틱스")
    );

    private static StockDTO s(String code, String name) {
        return StockDTO.builder().code(code).name(name).build();
    }
}
