package dev.pomyharry.stocksimulator.back.service;

import com.google.gson.*;
import dev.pomyharry.stocksimulator.back.component.CandleDataComponent;
import dev.pomyharry.stocksimulator.back.component.StockDataComponent;
import dev.pomyharry.stocksimulator.back.model.dto.ChatMessageDTO;
import dev.pomyharry.stocksimulator.back.model.dto.StockDTO;
import dev.pomyharry.stocksimulator.back.model.dto.StockDetailDTO;
import dev.pomyharry.stocksimulator.back.model.entity.Account;
import dev.pomyharry.stocksimulator.back.model.entity.ChatMessage;
import dev.pomyharry.stocksimulator.back.model.entity.Customer;
import dev.pomyharry.stocksimulator.back.model.entity.MyStock;
import dev.pomyharry.stocksimulator.back.model.entity.TradeHistory;
import dev.pomyharry.stocksimulator.back.repository.AccountRepository;
import dev.pomyharry.stocksimulator.back.repository.ChatMessageRepository;
import dev.pomyharry.stocksimulator.back.repository.CustomerRepository;
import dev.pomyharry.stocksimulator.back.repository.MyStockRepository;
import dev.pomyharry.stocksimulator.back.repository.TradeHistoryRepository;
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
        prompt.append("5. **개선 제안** - 리밸런싱, 분산투자, 리스크 관리\n");
        prompt.append("6. **점수** - 100점 만점 종합 점수와 근거\n\n");

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
        sb.append("당신은 '주린이집' 모의투자 앱의 AI 투자 어시스턴트입니다.\n");
        sb.append("한국어로 대화하며, 친절하고 전문적으로 주식 투자에 대해 안내합니다.\n");
        sb.append("모의투자 서비스이므로 실제 투자 손실 위험은 없습니다.\n\n");
        sb.append("역할:\n");
        sb.append("- 주식/ETF/선물 투자에 대한 기본적인 개념 설명\n");
        sb.append("- 사용자의 포트폴리오 분석 및 의견 제공\n");
        sb.append("- 매매 전략, 분산투자, 리스크 관리 등 투자 교육\n");
        sb.append("- 주린이집 앱 사용법 안내\n\n");
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
