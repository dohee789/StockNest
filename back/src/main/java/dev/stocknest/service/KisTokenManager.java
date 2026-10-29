package dev.stocknest.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stocknest.config.KisConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisTokenManager {

    private final KisConfig kisConfig;

    private volatile String accessToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    @PostConstruct
    public void init() {
        if (kisConfig.getAppKey() != null && !kisConfig.getAppKey().isBlank()) {
            refreshToken();
        } else {
            log.warn("KIS API key not configured — token will not be issued");
        }
    }

    public String getToken() {
        if (accessToken == null || Instant.now().isAfter(tokenExpiry.minusSeconds(3600))) {
            refreshToken();
        }
        return accessToken;
    }

    @Scheduled(fixedRate = 6 * 3600 * 1000) // every 6 hours
    public void scheduledRefresh() {
        if (kisConfig.getAppKey() != null && !kisConfig.getAppKey().isBlank()) {
            refreshToken();
        }
    }

    private synchronized void refreshToken() {
        try {
            // Double-check after acquiring lock
            if (accessToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(3600))) {
                return;
            }

            String urlStr = kisConfig.getBaseUrl() + "/oauth2/tokenP";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JsonObject body = new JsonObject();
            body.addProperty("grant_type", "client_credentials");
            body.addProperty("appkey", kisConfig.getAppKey());
            body.addProperty("appsecret", kisConfig.getAppSecret());

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String response = readStream(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream());

            if (status != 200) {
                log.error("KIS token request failed ({}): {}", status, response);
                return;
            }

            JsonObject result = JsonParser.parseString(response).getAsJsonObject();
            this.accessToken = result.get("access_token").getAsString();
            // Token valid for 24h, set expiry to 23h for safety margin
            this.tokenExpiry = Instant.now().plusSeconds(23 * 3600);

            log.info("KIS token issued successfully (expires ~23h)");

        } catch (Exception e) {
            log.error("KIS token refresh failed: {}", e.getMessage());
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
