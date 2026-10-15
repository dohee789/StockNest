package dev.pomyharry.stocksimulator.back.service;

import dev.pomyharry.stocksimulator.back.model.entity.Kakao;
import dev.pomyharry.stocksimulator.back.repository.KakaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMessageService {

    private final KakaoRepository kakaoRepository;

    /**
     * 카카오 '나에게 보내기' API를 이용해 메시지 발송
     * access_token이 없거나 만료된 경우 조용히 실패
     */
    public void sendToMe(String customerId, String title, String message) {
        // customer_id로 Kakao 엔티티 찾기 (kakao_id = customer.id)
        Kakao kakao = kakaoRepository.findById(customerId).orElse(null);
        if (kakao == null || kakao.getAccessToken() == null || kakao.getAccessToken().isBlank()) {
            log.debug("카카오 메시지 미발송 (access_token 없음): customerId={}", customerId);
            return;
        }

        try {
            String accessToken = kakao.getAccessToken();
            String templateObject = buildTextTemplate(title, message);

            URL url = new URL("https://kapi.kakao.com/v2/api/talk/memo/default/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setDoOutput(true);

            String body = "template_object=" + URLEncoder.encode(templateObject, StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                log.info("카카오 메시지 발송 성공: {}", title);
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    log.warn("카카오 메시지 발송 실패 [{}]: {}", responseCode, sb);
                }
            }
        } catch (Exception e) {
            log.warn("카카오 메시지 발송 오류: {}", e.getMessage());
        }
    }

    private String buildTextTemplate(String title, String message) {
        String text = "[" + title + "]\n" + message;
        return "{\"object_type\":\"text\",\"text\":" + jsonString(text)
                + ",\"link\":{\"web_url\":\"http://localhost:3000\",\"mobile_web_url\":\"http://localhost:3000\"}}";
    }

    private String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                + "\"";
    }
}
