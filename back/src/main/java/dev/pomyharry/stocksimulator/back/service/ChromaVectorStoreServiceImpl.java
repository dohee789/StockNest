package dev.pomyharry.stocksimulator.back.service;

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
import java.util.Map;

@Slf4j
@Service
public class ChromaVectorStoreServiceImpl implements VectorStoreService {

    @Value("${rag.chroma-url:http://localhost:8100}")
    private String chromaUrl;

    @Override
    public boolean isAvailable() {
        try {
            HttpURLConnection conn = openGet(chromaUrl + "/api/v1/heartbeat");
            int status = conn.getResponseCode();
            conn.disconnect();
            return status == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getOrCreateCollection(String name) {
        try {
            // 먼저 기존 컬렉션 조회
            HttpURLConnection getConn = openGet(chromaUrl + "/api/v1/collections/" + name);
            int getStatus = getConn.getResponseCode();
            if (getStatus == 200) {
                String body = readAll(getConn.getInputStream());
                getConn.disconnect();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                return json.get("id").getAsString();
            }
            getConn.disconnect();

            // 없으면 생성
            JsonObject reqBody = new JsonObject();
            reqBody.addProperty("name", name);
            reqBody.add("metadata", new JsonObject());

            HttpURLConnection conn = openPost(chromaUrl + "/api/v1/collections");
            writeBody(conn, reqBody);

            int status = conn.getResponseCode();
            String response = readAll(status >= 200 && status < 300
                    ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (status >= 200 && status < 300) {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                String id = json.get("id").getAsString();
                log.info("Chroma collection created: name={}, id={}", name, id);
                return id;
            } else {
                log.error("Failed to create collection: status={}, body={}", status, response);
                return null;
            }
        } catch (Exception e) {
            log.error("Chroma getOrCreateCollection failed", e);
            return null;
        }
    }

    @Override
    public void addDocuments(String collectionId, List<String> ids,
                             List<List<Float>> embeddings, List<String> documents,
                             List<Map<String, String>> metadatas) {
        try {
            JsonObject body = new JsonObject();
            body.add("ids", toJsonArray(ids));
            body.add("embeddings", toEmbeddingArray(embeddings));
            body.add("documents", toJsonArray(documents));

            JsonArray metaArr = new JsonArray();
            for (Map<String, String> m : metadatas) {
                JsonObject obj = new JsonObject();
                m.forEach(obj::addProperty);
                metaArr.add(obj);
            }
            body.add("metadatas", metaArr);

            String url = chromaUrl + "/api/v1/collections/" + collectionId + "/add";
            HttpURLConnection conn = openPost(url);
            writeBody(conn, body);

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                log.info("Added {} documents to Chroma", ids.size());
            } else {
                String err = readAll(conn.getErrorStream());
                log.error("Chroma add failed: status={}, body={}", status, err);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("Chroma addDocuments failed", e);
        }
    }

    @Override
    public List<String> query(String collectionId, List<Float> queryEmbedding, int nResults) {
        try {
            JsonObject body = new JsonObject();

            JsonArray qEmbArr = new JsonArray();
            JsonArray singleEmb = new JsonArray();
            for (Float f : queryEmbedding) singleEmb.add(f);
            qEmbArr.add(singleEmb);
            body.add("query_embeddings", qEmbArr);

            body.addProperty("n_results", nResults);
            body.add("include", parseArray("[\"documents\"]"));

            String url = chromaUrl + "/api/v1/collections/" + collectionId + "/query";
            HttpURLConnection conn = openPost(url);
            writeBody(conn, body);

            int status = conn.getResponseCode();
            String response = readAll(status >= 200 && status < 300
                    ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (status >= 200 && status < 300) {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonArray docs = json.getAsJsonArray("documents");
                List<String> results = new ArrayList<>();
                if (docs != null && !docs.isEmpty()) {
                    JsonArray firstResult = docs.get(0).getAsJsonArray();
                    for (JsonElement el : firstResult) {
                        results.add(el.getAsString());
                    }
                }
                return results;
            } else {
                log.error("Chroma query failed: status={}, body={}", status, response);
                return List.of();
            }
        } catch (Exception e) {
            log.error("Chroma query failed", e);
            return List.of();
        }
    }

    @Override
    public int count(String collectionId) {
        try {
            HttpURLConnection conn = openGet(
                    chromaUrl + "/api/v1/collections/" + collectionId + "/count");
            int status = conn.getResponseCode();
            String response = readAll(conn.getInputStream());
            conn.disconnect();
            if (status == 200) {
                return Integer.parseInt(response.trim());
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ── 유틸리티 ──

    private HttpURLConnection openGet(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        return conn;
    }

    private HttpURLConnection openPost(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        return conn;
    }

    private void writeBody(HttpURLConnection conn, JsonObject body) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
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

    private JsonArray toJsonArray(List<String> list) {
        JsonArray arr = new JsonArray();
        list.forEach(arr::add);
        return arr;
    }

    private JsonArray toEmbeddingArray(List<List<Float>> embeddings) {
        JsonArray outer = new JsonArray();
        for (List<Float> emb : embeddings) {
            JsonArray inner = new JsonArray();
            for (Float f : emb) inner.add(f);
            outer.add(inner);
        }
        return outer;
    }

    private JsonArray parseArray(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }
}
