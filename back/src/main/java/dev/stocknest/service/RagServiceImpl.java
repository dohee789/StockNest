package dev.stocknest.service;

import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @Value("${rag.collection-name:stock-knowledge}")
    private String collectionName;

    @Value("${rag.top-k:5}")
    private int topK;

    private String collectionId;
    private volatile boolean ready = false;

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                if (!vectorStoreService.isAvailable()) {
                    log.info("ChromaDB 미실행 — RAG 비활성, 챗봇은 정상 작동합니다.");
                    return;
                }
                initializeKnowledgeBase();
            } catch (Exception e) {
                log.warn("RAG 초기화 실패: {}", e.getMessage());
            }
        });
    }

    @Override
    public void initializeKnowledgeBase() {
        if (!vectorStoreService.isAvailable()) {
            log.warn("ChromaDB가 사용 불가합니다. RAG 기능이 비활성화됩니다.");
            return;
        }

        collectionId = vectorStoreService.getOrCreateCollection(collectionName);
        if (collectionId == null) {
            log.error("ChromaDB 컬렉션 생성 실패");
            return;
        }

        // 이미 데이터가 있으면 스킵
        int existingCount = vectorStoreService.count(collectionId);
        if (existingCount > 0) {
            log.info("지식 베이스 이미 로드됨 (문서 수: {})", existingCount);
            ready = true;
            return;
        }

        // JSON 파일에서 지식 문서 로드
        try {
            ClassPathResource resource = new ClassPathResource("knowledge/stock_knowledge.json");
            InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            JsonArray docs = JsonParser.parseReader(reader).getAsJsonArray();
            reader.close();

            List<String> ids = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            List<Map<String, String>> metadatas = new ArrayList<>();

            for (JsonElement el : docs) {
                JsonObject doc = el.getAsJsonObject();
                ids.add(doc.get("id").getAsString());
                contents.add(doc.get("content").getAsString());
                Map<String, String> meta = new HashMap<>();
                meta.put("category", doc.get("category").getAsString());
                metadatas.add(meta);
            }

            log.info("{}개 지식 문서 임베딩 중...", contents.size());
            List<List<Float>> embeddings = embeddingService.embedBatch(contents);

            // 빈 임베딩 필터링
            List<String> validIds = new ArrayList<>();
            List<List<Float>> validEmbeddings = new ArrayList<>();
            List<String> validContents = new ArrayList<>();
            List<Map<String, String>> validMetas = new ArrayList<>();

            for (int i = 0; i < ids.size(); i++) {
                if (!embeddings.get(i).isEmpty()) {
                    validIds.add(ids.get(i));
                    validEmbeddings.add(embeddings.get(i));
                    validContents.add(contents.get(i));
                    validMetas.add(metadatas.get(i));
                }
            }

            if (!validIds.isEmpty()) {
                vectorStoreService.addDocuments(collectionId, validIds, validEmbeddings, validContents, validMetas);
                log.info("지식 베이스 초기화 완료: {}개 문서 로드됨", validIds.size());
                ready = true;
            } else {
                log.error("임베딩 생성 실패: 유효한 문서가 없습니다.");
            }
        } catch (Exception e) {
            log.error("지식 베이스 로드 실패", e);
        }
    }

    @Override
    public String retrieveContext(String query) {
        if (!ready || collectionId == null) {
            return "";
        }

        try {
            List<Float> queryEmbedding = embeddingService.embed(query);
            if (queryEmbedding.isEmpty()) return "";

            List<String> results = vectorStoreService.query(collectionId, queryEmbedding, topK);
            if (results.isEmpty()) return "";

            StringBuilder context = new StringBuilder();
            context.append("=== 관련 금융 지식 (RAG) ===\n");
            for (int i = 0; i < results.size(); i++) {
                context.append("[").append(i + 1).append("] ").append(results.get(i)).append("\n\n");
            }
            return context.toString();
        } catch (Exception e) {
            log.error("RAG 검색 실패", e);
            return "";
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }
}
