package dev.pomyharry.stocksimulator.back.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pomyharry.stocksimulator.back.model.dto.MarketIndexDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MarketIndexComponent {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    public List<MarketIndexDTO> getMarketIndexes() {
        List<MarketIndexDTO> result = new ArrayList<>();

        MarketIndexDTO kospi = fetchIndex("KOSPI", "%5EKS11");   // ^KS11
        result.add(kospi != null ? kospi : new MarketIndexDTO("KOSPI", "-", "-", "-", false));

        MarketIndexDTO kosdaq = fetchIndex("KOSDAQ", "%5EKQ11"); // ^KQ11
        result.add(kosdaq != null ? kosdaq : new MarketIndexDTO("KOSDAQ", "-", "-", "-", false));

        return result;
    }

    private MarketIndexDTO fetchIndex(String name, String encodedSymbol) {
        try {
            String urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/"
                    + encodedSymbol + "?interval=1d&range=1d";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
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

            DecimalFormat dfVal = new DecimalFormat("#,##0.00");
            DecimalFormat dfChg = new DecimalFormat("0.00");

            String valueStr = dfVal.format(price);
            String changeStr = (changeVal >= 0 ? "+" : "") + dfChg.format(changeVal);
            String changeRateStr = String.format("%+.2f%%", changeRate);

            return new MarketIndexDTO(name, valueStr, changeStr, changeRateStr, changeVal >= 0);

        } catch (Exception e) {
            log.debug("Yahoo 지수 조회 실패 [{}]: {}", name, e.getMessage());
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
}
