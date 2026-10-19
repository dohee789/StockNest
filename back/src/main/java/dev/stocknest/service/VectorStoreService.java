package dev.stocknest.service;

import java.util.List;
import java.util.Map;

public interface VectorStoreService {

    /** 컬렉션 생성 (이미 존재하면 기존 ID 반환) */
    String getOrCreateCollection(String name);

    /** 문서 추가 */
    void addDocuments(String collectionId, List<String> ids,
                      List<List<Float>> embeddings, List<String> documents,
                      List<Map<String, String>> metadatas);

    /** 벡터 유사도 검색 */
    List<String> query(String collectionId, List<Float> queryEmbedding, int nResults);

    /** 컬렉션 내 문서 수 조회 */
    int count(String collectionId);

    /** 헬스 체크 */
    boolean isAvailable();
}
