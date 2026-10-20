package dev.stocknest.service;

import com.google.gson.*;
import dev.stocknest.component.CandleDataComponent;
import dev.stocknest.component.SiseRankComponent;
import dev.stocknest.component.StockDataComponent;
import dev.stocknest.model.dto.ChatMessageDTO;
import dev.stocknest.model.dto.SiseRankDTO;
import dev.stocknest.model.dto.StockDTO;
import dev.stocknest.model.dto.StockDetailDTO;
import dev.stocknest.model.dto.StockRecommendationDTO;
import dev.stocknest.model.entity.Account;
import dev.stocknest.model.entity.ChatMessage;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.model.entity.MyStock;
import dev.stocknest.model.entity.TradeHistory;
import dev.stocknest.repository.AccountRepository;
import dev.stocknest.repository.ChatMessageRepository;
import dev.stocknest.repository.CustomerRepository;
import dev.stocknest.repository.MyStockRepository;
import dev.stocknest.repository.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotServiceImpl.class);
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_HISTORY = 20;
    private static final DecimalFormat KRW_FMT = new DecimalFormat("#,###");
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    @Value("${chatbot.api-key:}")
    private String apiKey;

    @Value("${chatbot.model:gemini-3.6-flash}")
    private String model;

    private final ChatMessageRepository chatMessageRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final MyStockRepository myStockRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final CandleDataComponent candleDataComponent;
    private final StockDataComponent stockDataComponent;
    private final SiseRankComponent siseRankComponent;
    private final RagService ragService;

    // ─── 기존 챗봇 (비스트리밍) ───

    @Override
    @Transactional
    public ChatMessageDTO chat(String customerId, String userMessage) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));

        saveUserMessage(customer, userMessage);
        String systemPrompt = buildChatSystemPrompt(customer, customerId, userMessage);
        JsonArray contents = buildGeminiContents(customerId);
        String assistantContent = callGemini(systemPrompt, contents);

        ChatMessage assistantMsg = ChatMessage.builder()
                .role("assistant").content(assistantContent)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .customer(customer).build();
        chatMessageRepository.save(assistantMsg);

        return ChatMessageDTO.builder()
                .id(assistantMsg.getId()).role("assistant")
                .content(assistantContent).createdAt(assistantMsg.getCreatedAt())
                .build();
    }

    // ─── 스트리밍 챗봇 ───

    @Override
    public SseEmitter chatStream(String customerId, String userMessage) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2분 타임아웃

        streamExecutor.execute(() -> {
            try {
                Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));

                saveUserMessage(customer, userMessage);
                String systemPrompt = buildChatSystemPrompt(customer, customerId, userMessage);
                JsonArray contents = buildGeminiContents(customerId);

                StringBuilder fullResponse = new StringBuilder();
                streamGemini(systemPrompt, contents, emitter, fullResponse);

                // 전체 응답 DB 저장
                ChatMessage assistantMsg = ChatMessage.builder()
                        .role("assistant").content(fullResponse.toString())
                        .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                        .customer(customer).build();
                chatMessageRepository.save(assistantMsg);

                emitter.send(SseEmitter.event().name("done").data(assistantMsg.getId()));
                emitter.complete();
            } catch (Exception e) {
                log.error("Stream failed", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("스트리밍 중 오류가 발생했습니다."));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // ─── 종목 AI 분석 ───

    @Override
    public String analyzeStock(String customerId, String stockCode) {
        // 종목 상세 정보 조회
        StockDTO current = stockDataComponent.getStockChart(stockCode, null);
        StockDetailDTO detail = StockDetailDTO.builder()
                .code(stockCode).name(stockCode)
                .price(current != null ? current.getPrice() : "-")
                .change(current != null ? current.getChange() : "-")
                .changeRate(current != null ? current.getChangeRate() : "-")
                .build();
        try { candleDataComponent.enrichStockDetail(detail); } catch (Exception ignored) {}

        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 한국 주식 시장 전문 AI 애널리스트입니다.\n");
        prompt.append("아래 종목 데이터를 분석하여 투자 참고 의견을 제공하세요.\n");
        prompt.append("모의투자용이므로 적극적으로 분석 의견을 제시해도 됩니다.\n");
        prompt.append("한국어로 답변하고, 마크다운 형식을 사용하세요.\n\n");

        prompt.append("## 분석 항목\n");
        prompt.append("1. **현재 상태 요약** - 현재가, 등락률, 거래량 기반\n");
        prompt.append("2. **기술적 분석** - 52주 고저 대비 위치, 최근 추세\n");
        prompt.append("3. **밸류에이션** - PER/PBR 기반 적정가 판단\n");
        prompt.append("4. **매매 전략 제안** - 진입/청산 시점, 물량 배분\n");
        prompt.append("5. **리스크 요인** - 주의할 점\n\n");

        prompt.append("=== 종목 데이터 ===\n");
        if (detail != null) {
            prompt.append("종목: ").append(detail.getName()).append(" (").append(detail.getCode()).append(")\n");
            prompt.append("현재가: ").append(detail.getPrice()).append("\n");
            prompt.append("등락: ").append(detail.getChange()).append(" (").append(detail.getChangeRate()).append(")\n");
            prompt.append("거래량: ").append(detail.getVolume()).append("\n");
            prompt.append("52주 최고: ").append(detail.getHigh52w()).append("\n");
            prompt.append("52주 최저: ").append(detail.getLow52w()).append("\n");
            prompt.append("PER: ").append(detail.getPer()).append("\n");
            prompt.append("PBR: ").append(detail.getPbr()).append("\n");
        } else if (current != null) {
            prompt.append("종목: ").append(current.getName()).append(" (").append(current.getCode()).append(")\n");
            prompt.append("현재가: ").append(current.getPrice()).append("\n");
            prompt.append("등락: ").append(current.getChange()).append(" (").append(current.getChangeRate()).append(")\n");
        } else {
            return "종목 데이터를 조회할 수 없습니다.";
        }

        // 사용자 보유 정보
        if (customerId != null) {
            List<MyStock> myStocks = myStockRepository.findAllByCustomerId(customerId);
            myStocks.stream()
                    .filter(s -> s.getStockCode().equals(stockCode))
                    .findFirst()
                    .ifPresent(s -> {
                        long avgPrice = s.getAmount() > 0 ? s.getTotalBuyPrice() / s.getAmount() : 0;
                        prompt.append("\n=== 내 보유 현황 ===\n");
                        prompt.append("보유수량: ").append(s.getAmount()).append("주\n");
                        prompt.append("평균단가: ").append(KRW_FMT.format(avgPrice)).append("원\n");
                    });
        }

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", "이 종목을 분석해주세요.");
        parts.add(part);
        userContent.add("parts", parts);
        contents.add(userContent);

        return callGemini(prompt.toString(), contents);
    }

    // ─── 포트폴리오 AI 리포트 ───

    @Override
    public String generatePortfolioReport(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));
        Account account = accountRepository.findByCustomerId(customerId);
        List<MyStock> stocks = myStockRepository.findAllByCustomerId(customerId);
        List<TradeHistory> trades = tradeHistoryRepository
                .findAllByCustomerIdOrderByTradeTimeDesc(customerId);

        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 전문 투자 포트폴리오 분석가입니다.\n");
        prompt.append("아래 모의투자 포트폴리오를 종합 분석하여 리포트를 작성하세요.\n");
        prompt.append("한국어, 마크다운 형식으로 작성하세요.\n\n");

        prompt.append("## 리포트 구성\n");
        prompt.append("1. **포트폴리오 총평** - 전체적인 건전성, 수익률 평가\n");
        prompt.append("2. **자산 배분 분석** - 예수금 vs 투자금 비율, 종목 집중도\n");
        prompt.append("3. **종목별 평가** - 각 보유 종목의 수익률, 비중\n");
        prompt.append("4. **매매 패턴 분석** - 최근 거래 기반 투자 습관\n");
        prompt.append("5. **리스크 분석** - 포트폴리오 변동성, 집중 위험도, 최대 손실 가능성\n");
        prompt.append("6. **분산투자 평가** - 종목 수, 섹터 다양성, 시가총액 분포 기반 분산도\n");
        prompt.append("7. **섹터 분포** - 각 보유 종목의 업종 분류 (IT, 금융, 바이오 등) 및 비중\n");
        prompt.append("8. **개선 제안** - 리밸런싱, 분산투자, 리스크 관리\n");
        prompt.append("9. **점수** - 100점 만점 종합 점수와 근거\n\n");

        prompt.append("## 중요: 구조화 데이터 태그\n");
        prompt.append("리포트 맨 마지막에 반드시 아래 태그를 각각 한 줄씩 포함하세요:\n");
        prompt.append("[RISK_SCORE:X/10] — 리스크 점수 (1=매우 안전, 10=매우 위험)\n");
        prompt.append("[DIVERSIFICATION:X/10] — 분산투자 점수 (1=매우 집중, 10=매우 분산)\n");
        prompt.append("[SECTOR:{\"IT\":30,\"금융\":25,\"바이오\":20,\"기타\":25}] — 섹터별 비중(%) JSON\n\n");

        prompt.append("=== 계좌 정보 ===\n");
        if (account != null) {
            prompt.append("예수금: ").append(KRW_FMT.format(account.getDeposit())).append("원\n");
            prompt.append("시드머니: ").append(KRW_FMT.format(account.getSeedMoney())).append("원\n");
            long pnl = account.getDeposit() - account.getSeedMoney();
            prompt.append("실현 손익: ").append(pnl >= 0 ? "+" : "").append(KRW_FMT.format(pnl)).append("원\n");
        }

        prompt.append("\n=== 보유 종목 ===\n");
        if (stocks.isEmpty()) {
            prompt.append("보유 종목 없음\n");
        } else {
            long totalInvested = 0;
            for (MyStock s : stocks) {
                long avgPrice = s.getAmount() > 0 ? s.getTotalBuyPrice() / s.getAmount() : 0;
                prompt.append("- ").append(s.getStockName()).append("(").append(s.getStockCode()).append("): ");
                prompt.append(s.getAmount()).append("주, 평균단가 ").append(KRW_FMT.format(avgPrice)).append("원, ");
                prompt.append("총 투자금 ").append(KRW_FMT.format(s.getTotalBuyPrice())).append("원\n");
                totalInvested += s.getTotalBuyPrice();
            }
            prompt.append("총 투자금: ").append(KRW_FMT.format(totalInvested)).append("원\n");
        }

        prompt.append("\n=== 최근 거래 내역 (최대 20건) ===\n");
        if (trades.isEmpty()) {
            prompt.append("거래 내역 없음\n");
        } else {
            trades.stream().limit(20).forEach(t -> {
                prompt.append("- ").append(t.getTradeTime().format(DTF)).append(" ");
                prompt.append(t.getTradeType().equals("BUY") ? "매수" : "매도").append(" ");
                prompt.append(t.getStockName()).append(" ").append(t.getAmount()).append("주 ");
                prompt.append("@").append(KRW_FMT.format(t.getPricePerStock())).append("원\n");
            });
        }

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", "내 포트폴리오를 분석해서 리포트를 작성해줘.");
        parts.add(part);
        userContent.add("parts", parts);
        contents.add(userContent);

        return callGemini(prompt.toString(), contents);
    }

    // ─── 마켓 인사이트 ───

    @Override
    public String generateMarketInsight(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));
        Account account = accountRepository.findByCustomerId(customerId);
        List<MyStock> stocks = myStockRepository.findAllByCustomerId(customerId);

        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 증권사 AI 어드바이저입니다.\n");
        prompt.append("사용자의 포트폴리오를 기반으로 짧은 마켓 인사이트 알림 메시지를 생성하세요.\n");
        prompt.append("한국어로 300자 이내, 친근한 톤으로 작성하세요.\n");
        prompt.append("이모지를 사용하고, 핵심 포인트만 전달하세요.\n\n");

        prompt.append("=== 사용자 ===\n");
        prompt.append("이름: ").append(customer.getName()).append("\n");

        if (account != null) {
            prompt.append("예수금: ").append(KRW_FMT.format(account.getDeposit())).append("원\n");
            long pnl = account.getDeposit() - account.getSeedMoney();
            prompt.append("수익률: ").append(String.format("%.1f%%", (double) pnl / account.getSeedMoney() * 100)).append("\n");
        }

        if (!stocks.isEmpty()) {
            prompt.append("\n보유 종목: ");
            prompt.append(stocks.stream().map(MyStock::getStockName).collect(Collectors.joining(", ")));
            prompt.append("\n");
        }

        prompt.append("\n다음 중 하나의 주제로 알림 메시지를 작성하세요:\n");
        prompt.append("- 보유 종목에 대한 오늘의 체크 포인트\n");
        prompt.append("- 포트폴리오 리밸런싱 팁\n");
        prompt.append("- 투자 격언이나 교훈\n");
        prompt.append("- 분산투자/리스크 관리 조언\n");

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", "오늘의 마켓 인사이트를 알려줘.");
        parts.add(part);
        userContent.add("parts", parts);
        contents.add(userContent);

        return callGemini(prompt.toString(), contents);
    }

    // ─── AI 맞춤 종목 추천 ───

    @Override
    public List<StockRecommendationDTO> generateStockRecommendations(String customerId) {
        Account account = accountRepository.findByCustomerId(customerId);
        List<MyStock> stocks = myStockRepository.findAllByCustomerId(customerId);
        List<TradeHistory> trades = tradeHistoryRepository
                .findAllByCustomerIdOrderByTradeTimeDesc(customerId);
        List<SiseRankDTO> rising = siseRankComponent.getRising();
        List<SiseRankDTO> falling = siseRankComponent.getFalling();

        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 한국 주식 시장 전문 AI 애널리스트입니다.\n");
        prompt.append("사용자의 포트폴리오와 시장 데이터를 분석하여 맞춤 종목 3~5개를 추천하세요.\n");
        prompt.append("모의투자용이므로 적극적으로 추천해도 됩니다.\n\n");
        prompt.append("반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.\n");
        prompt.append("```json\n[\n  {\n    \"stockCode\": \"005930\",\n    \"stockName\": \"삼성전자\",\n");
        prompt.append("    \"reason\": \"추천 이유 (한국어, 1~2문장)\",\n");
        prompt.append("    \"confidence\": \"high|medium|low\",\n");
        prompt.append("    \"action\": \"buy|watch\"\n  }\n]\n```\n\n");

        prompt.append("추천 기준:\n");
        prompt.append("- 사용자가 이미 보유한 종목은 제외\n");
        prompt.append("- 사용자의 투자 성향(보유 종목, 매매 패턴) 고려\n");
        prompt.append("- 현재 시장 급등/급락 종목 중 유망한 것 포함\n");
        prompt.append("- 분산투자를 위해 다양한 업종에서 추천\n\n");

        prompt.append("=== 사용자 보유 종목 ===\n");
        if (stocks.isEmpty()) {
            prompt.append("보유 종목 없음 (초보 투자자 — 안정적인 대형주 위주 추천)\n");
        } else {
            for (MyStock s : stocks) {
                prompt.append("- ").append(s.getStockName()).append("(").append(s.getStockCode()).append(")\n");
            }
        }

        if (account != null) {
            prompt.append("\n가용 예수금: ").append(KRW_FMT.format(account.getDeposit())).append("원\n");
        }

        prompt.append("\n=== 오늘의 급등 종목 ===\n");
        rising.stream().limit(10).forEach(r ->
                prompt.append("- ").append(r.getName()).append("(").append(r.getCode()).append(") ")
                        .append(r.getPrice()).append("원 ").append(r.getChangeRate()).append("\n"));

        prompt.append("\n=== 오늘의 급락 종목 ===\n");
        falling.stream().limit(10).forEach(f ->
                prompt.append("- ").append(f.getName()).append("(").append(f.getCode()).append(") ")
                        .append(f.getPrice()).append("원 ").append(f.getChangeRate()).append("\n"));

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", "내 포트폴리오에 맞는 종목을 추천해줘.");
        parts.add(part);
        userContent.add("parts", parts);
        contents.add(userContent);

        String response = callGemini(prompt.toString(), contents);
        return parseRecommendations(response);
    }

    private List<StockRecommendationDTO> parseRecommendations(String response) {
        List<StockRecommendationDTO> result = new ArrayList<>();
        try {
            // Extract JSON from markdown code block if present
            String json = response;
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                json = json.substring(0, json.indexOf("```"));
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                json = json.substring(0, json.indexOf("```"));
            }
            json = json.trim();

            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                result.add(StockRecommendationDTO.builder()
                        .stockCode(getJsonStr(obj, "stockCode"))
                        .stockName(getJsonStr(obj, "stockName"))
                        .reason(getJsonStr(obj, "reason"))
                        .confidence(getJsonStr(obj, "confidence"))
                        .action(getJsonStr(obj, "action"))
                        .build());
            }
        } catch (Exception e) {
            log.warn("추천 JSON 파싱 실패, raw 텍스트 폴백: {}", e.getMessage());
            result.add(StockRecommendationDTO.builder()
                    .stockName("AI 추천")
                    .reason(response)
                    .confidence("medium")
                    .action("watch")
                    .build());
        }
        return result;
    }

    private String getJsonStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    // ─── AI 시장 브리핑 ───

    @Override
    public String generateMarketBriefing(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));
        Account account = accountRepository.findByCustomerId(customerId);
        List<MyStock> stocks = myStockRepository.findAllByCustomerId(customerId);
        List<SiseRankDTO> rising = siseRankComponent.getRising();
        List<SiseRankDTO> falling = siseRankComponent.getFalling();

        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 증권사 AI 어드바이저입니다.\n");
        prompt.append("사용자의 보유 종목과 오늘의 시장 데이터를 기반으로 개인화된 시장 브리핑을 작성하세요.\n");
        prompt.append("한국어 마크다운 형식, 500~800자, 친근하지만 전문적인 톤으로 작성하세요.\n\n");

        prompt.append("## 브리핑 구성\n");
        prompt.append("1. **오늘의 시장** - 전체 시장 분위기 (급등/급락 종목 기반으로 유추)\n");
        prompt.append("2. **내 종목 체크** - 사용자 보유 종목 관련 시장 동향\n");
        prompt.append("3. **오늘의 관전 포인트** - 오늘 주목할 만한 1가지\n\n");

        prompt.append("=== 사용자: ").append(customer.getName()).append(" ===\n");

        if (!stocks.isEmpty()) {
            prompt.append("보유 종목: ");
            prompt.append(stocks.stream().map(s -> s.getStockName() + "(" + s.getStockCode() + ")")
                    .collect(Collectors.joining(", ")));
            prompt.append("\n");
        } else {
            prompt.append("보유 종목 없음 (초보 투자자)\n");
        }

        prompt.append("\n=== 오늘의 급등 top 10 ===\n");
        rising.stream().limit(10).forEach(r ->
                prompt.append("- ").append(r.getName()).append(" ").append(r.getChangeRate()).append("\n"));

        prompt.append("\n=== 오늘의 급락 top 10 ===\n");
        falling.stream().limit(10).forEach(f ->
                prompt.append("- ").append(f.getName()).append(" ").append(f.getChangeRate()).append("\n"));

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", "오늘의 시장 브리핑을 작성해줘.");
        parts.add(part);
        userContent.add("parts", parts);
        contents.add(userContent);

        return callGemini(prompt.toString(), contents);
    }

    // ─── AI 매매 신호 ───

    @Override
    public String generateTradeSignals(String customerId) {
        List<MyStock> stocks = myStockRepository.findAllByCustomerId(customerId);
        List<SiseRankDTO> rising = siseRankComponent.getRising();
        List<SiseRankDTO> falling = siseRankComponent.getFalling();

        if (stocks.isEmpty()) {
            return "보유 종목이 없어 매매 신호를 생성할 수 없습니다.";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 AI 트레이딩 신호 분석가입니다.\n");
        prompt.append("사용자의 보유 종목을 분석하여 매매 신호를 제공하세요.\n");
        prompt.append("한국어 마크다운 형식으로, 각 종목별 신호를 작성하세요.\n\n");

        prompt.append("## 각 종목별 형식\n");
        prompt.append("### 종목명 (종목코드)\n");
        prompt.append("- **신호**: 추가매수 / 보유유지 / 매도검토\n");
        prompt.append("- **근거**: 1~2문장\n");
        prompt.append("- **긴급도**: 일반 / 주의 / 긴급\n\n");

        prompt.append("=== 보유 종목 ===\n");
        for (MyStock s : stocks) {
            long avgPrice = s.getAmount() > 0 ? s.getTotalBuyPrice() / s.getAmount() : 0;
            prompt.append("- ").append(s.getStockName()).append("(").append(s.getStockCode()).append("): ");
            prompt.append(s.getAmount()).append("주, 평균단가 ").append(KRW_FMT.format(avgPrice)).append("원\n");
        }

        prompt.append("\n=== 시장 급등 종목 ===\n");
        rising.stream().limit(5).forEach(r ->
                prompt.append("- ").append(r.getName()).append(" ").append(r.getChangeRate()).append("\n"));

        prompt.append("\n=== 시장 급락 종목 ===\n");
        falling.stream().limit(5).forEach(f ->
                prompt.append("- ").append(f.getName()).append(" ").append(f.getChangeRate()).append("\n"));

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", "내 보유 종목에 대한 매매 신호를 분석해줘.");
        parts.add(part);
        userContent.add("parts", parts);
        contents.add(userContent);

        return callGemini(prompt.toString(), contents);
    }

    // ─── 기록 조회/삭제 ───

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getHistory(String customerId) {
        return chatMessageRepository.findAllByCustomerIdOrderByCreatedAtAsc(customerId).stream()
                .map(m -> ChatMessageDTO.builder()
                        .id(m.getId()).role(m.getRole())
                        .content(m.getContent()).createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearHistory(String customerId) {
        chatMessageRepository.deleteAllByCustomerId(customerId);
    }

    // ═══════════════════════════════════════════
    //  내부 메서드
    // ═══════════════════════════════════════════

    @Transactional
    protected void saveUserMessage(Customer customer, String userMessage) {
        ChatMessage userMsg = ChatMessage.builder()
                .role("user").content(userMessage)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .customer(customer).build();
        chatMessageRepository.save(userMsg);
    }

    private static final String[] FALLBACK_MODELS = {
            "gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.1-flash-lite"
    };

    /** Gemini 비스트리밍 호출 (재시도 + 폴백 모델) */
    private String callGemini(String systemPrompt, JsonArray contents) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI 서비스가 설정되지 않았습니다.";
        }

        // 1차: 기본 모델로 최대 2회 재시도
        String result = callGeminiWithRetry(model, systemPrompt, contents, 2);
        if (result != null) return result;

        // 2차: 폴백 모델 시도
        for (String fallbackModel : FALLBACK_MODELS) {
            log.info("기본 모델({}) 실패, 폴백 모델({}) 시도", model, fallbackModel);
            result = callGeminiWithRetry(fallbackModel, systemPrompt, contents, 1);
            if (result != null) return result;
        }

        return "AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.";
    }

    private String callGeminiWithRetry(String targetModel, String systemPrompt,
                                        JsonArray contents, int maxRetries) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    Thread.sleep(1000L * attempt); // 1초, 2초 대기
                }
                JsonObject requestBody = buildRequestBody(systemPrompt, contents);
                String url = API_URL + targetModel + ":generateContent?key=" + apiKey;

                HttpURLConnection conn = openConnection(url);
                writeBody(conn, requestBody);

                int status = conn.getResponseCode();
                InputStream stream = (status >= 200 && status < 300)
                        ? conn.getInputStream() : conn.getErrorStream();
                String responseBody = readAll(stream);
                conn.disconnect();

                if (status >= 200 && status < 300) {
                    return extractGeminiText(responseBody);
                } else if (status == 503 || status == 429) {
                    log.warn("Gemini API {} (model={}, attempt={}/{}): {}",
                            status, targetModel, attempt + 1, maxRetries + 1, responseBody);
                    continue; // 재시도
                } else {
                    log.warn("Gemini API error: status={}, model={}, body={}",
                            status, targetModel, responseBody);
                    return null; // 4xx 등은 재시도 불필요
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                log.error("Gemini API call failed (model={})", targetModel, e);
                return null;
            }
        }
        return null; // 모든 재시도 실패
    }

    /** Gemini 스트리밍 호출 → SSE chunk를 emitter로 전달 (폴백 모델 지원) */
    private void streamGemini(String systemPrompt, JsonArray contents,
                              SseEmitter emitter, StringBuilder fullResponse) throws IOException {
        // 기본 모델 → 폴백 모델 순서로 시도
        String[] modelsToTry = new String[FALLBACK_MODELS.length + 1];
        modelsToTry[0] = model;
        System.arraycopy(FALLBACK_MODELS, 0, modelsToTry, 1, FALLBACK_MODELS.length);

        for (String targetModel : modelsToTry) {
            JsonObject requestBody = buildRequestBody(systemPrompt, contents);
            String url = API_URL + targetModel + ":streamGenerateContent?alt=sse&key=" + apiKey;

            HttpURLConnection conn = openConnection(url);
            writeBody(conn, requestBody);

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) continue;
                        String jsonStr = line.substring(6).trim();
                        if (jsonStr.isEmpty()) continue;

                        try {
                            String text = extractGeminiText(jsonStr);
                            if (text != null && !text.isEmpty()) {
                                fullResponse.append(text);
                                emitter.send(SseEmitter.event().name("token").data(text));
                            }
                        } catch (Exception e) {
                            log.debug("Stream chunk parse error: {}", e.getMessage());
                        }
                    }
                }
                conn.disconnect();
                return; // 성공
            }

            String errorBody = readAll(conn.getErrorStream());
            conn.disconnect();
            if (status == 503 || status == 429) {
                log.warn("Gemini stream {} (model={}), 폴백 시도", status, targetModel);
                continue; // 다음 모델 시도
            }
            log.warn("Gemini stream error: status={}, model={}, body={}", status, targetModel, errorBody);
            break;
        }

        emitter.send(SseEmitter.event().name("error").data("AI 서비스가 일시적으로 불안정합니다."));
    }

    private JsonObject buildRequestBody(String systemPrompt, JsonArray contents) {
        JsonObject systemInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemPrompt);
        sysParts.add(sysPart);
        systemInstruction.add("parts", sysParts);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", 4096);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("systemInstruction", systemInstruction);
        body.add("generationConfig", generationConfig);
        return body;
    }

    private HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        return conn;
    }

    private void writeBody(HttpURLConnection conn, JsonObject body) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private String extractGeminiText(String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray candidates = json.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) return "";

        JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
        if (content == null) return "";

        JsonArray parts = content.getAsJsonArray("parts");
        if (parts == null || parts.isEmpty()) return "";

        // thinking이 아닌 실제 답변만 추출
        for (int i = parts.size() - 1; i >= 0; i--) {
            JsonObject part = parts.get(i).getAsJsonObject();
            if (!part.has("thought") && part.has("text")) {
                return part.get("text").getAsString();
            }
        }
        return "";
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

    private String buildChatSystemPrompt(Customer customer, String customerId, String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 'StockNest' 모의투자 앱의 AI 투자 어시스턴트입니다.\n");
        sb.append("한국어로 대화하며, 친절하고 전문적으로 주식 투자에 대해 안내합니다.\n");
        sb.append("모의투자 서비스이므로 실제 투자 손실 위험은 없습니다.\n\n");
        sb.append("역할:\n");
        sb.append("- 주식/ETF/선물 투자에 대한 기본적인 개념 설명\n");
        sb.append("- 사용자의 포트폴리오 분석 및 의견 제공\n");
        sb.append("- 매매 전략, 분산투자, 리스크 관리 등 투자 교육\n");
        sb.append("- StockNest 앱 사용법 안내\n\n");
        sb.append("주의사항:\n");
        sb.append("- 특정 종목 매수/매도를 강력히 추천하지 마세요. 참고 의견만 제공하세요.\n");
        sb.append("- 응답은 간결하게(800자 이내) 작성하세요.\n");
        sb.append("- 이모지를 적절히 사용하여 친근한 톤을 유지하세요.\n");
        sb.append("- 아래 '관련 금융 지식'이 제공된 경우, 이를 참고하여 정확한 답변을 하세요.\n\n");

        // RAG: 사용자 질문 기반 관련 지식 검색
        String ragContext = ragService.retrieveContext(userMessage);
        if (!ragContext.isEmpty()) {
            sb.append(ragContext).append("\n");
        }

        sb.append("=== 사용자 정보 ===\n");
        sb.append("이름: ").append(customer.getName()).append("\n");

        Account account = accountRepository.findByCustomerId(customerId);
        if (account != null) {
            sb.append("\n=== 계좌 정보 ===\n");
            sb.append("예수금: ").append(KRW_FMT.format(account.getDeposit())).append("원\n");
            sb.append("시드머니: ").append(KRW_FMT.format(account.getSeedMoney())).append("원\n");
            long pnl = account.getDeposit() - account.getSeedMoney();
            sb.append("실현 손익: ").append(pnl >= 0 ? "+" : "").append(KRW_FMT.format(pnl)).append("원\n");
        }

        List<MyStock> stocks = myStockRepository.findAllByCustomerId(customerId);
        if (!stocks.isEmpty()) {
            sb.append("\n=== 보유 종목 ===\n");
            for (MyStock s : stocks) {
                long avgPrice = s.getAmount() > 0 ? s.getTotalBuyPrice() / s.getAmount() : 0;
                sb.append("- ").append(s.getStockName()).append("(").append(s.getStockCode()).append("): ");
                sb.append(s.getAmount()).append("주, 평균단가 ").append(KRW_FMT.format(avgPrice)).append("원\n");
            }
        } else {
            sb.append("\n보유 종목 없음\n");
        }

        List<TradeHistory> trades = tradeHistoryRepository
                .findAllByCustomerIdOrderByTradeTimeDesc(customerId);
        if (!trades.isEmpty()) {
            sb.append("\n=== 최근 거래 (최대 5건) ===\n");
            trades.stream().limit(5).forEach(t -> {
                sb.append("- ").append(t.getTradeTime().format(DTF)).append(" ");
                sb.append(t.getTradeType().equals("BUY") ? "매수" : "매도").append(" ");
                sb.append(t.getStockName()).append(" ").append(t.getAmount()).append("주 ");
                sb.append("@").append(KRW_FMT.format(t.getPricePerStock())).append("원\n");
            });
        }

        return sb.toString();
    }

    private JsonArray buildGeminiContents(String customerId) {
        List<ChatMessage> history = chatMessageRepository
                .findAllByCustomerIdOrderByCreatedAtAsc(customerId);

        int start = Math.max(0, history.size() - MAX_HISTORY);
        List<ChatMessage> recent = history.subList(start, history.size());

        JsonArray contents = new JsonArray();
        for (ChatMessage msg : recent) {
            JsonObject content = new JsonObject();
            String role = msg.getRole().equals("assistant") ? "model" : "user";
            content.addProperty("role", role);

            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", msg.getContent());
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
        }
        return contents;
    }
}
