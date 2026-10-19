package dev.stocknest.service;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${chatbot.api-key:}")
    private String apiKey;

    @Value("${rag.embedding-model:text-embedding-004}")
    private String embeddingModel;

    @Override
    public List<Float> embed(String text) {
        try {
            String url = API_URL + embeddingModel + ":embedContent?key=" + apiKey;

            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", text);
            parts.add(part);
            content.add("parts", parts);

            JsonObject body = new JsonObject();
            body.add("content", content);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream stream = (status >= 200 && status < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String response = readAll(stream);
            conn.disconnect();

            if (status >= 200 && status < 300) {
                return parseEmbedding(response);
            } else {
                log.error("Embedding API error: status={}, body={}", status, response);
                return List.of();
            }
        } catch (Exception e) {
            log.error("Embedding failed", e);
            return List.of();
        }
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        List<List<Float>> results = new ArrayList<>();
        // Gemini embedContent는 단일 요청만 지원하므로 순차 처리
        for (String text : texts) {
            results.add(embed(text));
            // Rate limit 방지
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        return results;
    }

    private List<Float> parseEmbedding(String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject embedding = json.getAsJsonObject("embedding");
        if (embedding == null) return List.of();

        JsonArray values = embedding.getAsJsonArray("values");
        List<Float> result = new ArrayList<>(values.size());
        for (JsonElement v : values) {
            result.add(v.getAsFloat());
        }
        return result;
    }

    private String readAll(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
