package dev.pomyharry.stocksimulator.back.service;

import java.util.List;

public interface RagService {

    /** 지식 베이스 초기화 (앱 시작 시 호출) */
    void initializeKnowledgeBase();

    /** 사용자 질문에 대해 관련 지식을 검색하여 컨텍스트로 반환 */
    String retrieveContext(String query);

    /** RAG 상태 확인 */
    boolean isReady();
}
