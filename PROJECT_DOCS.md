# StockSimulator — 개발자 문서

> 모의주식 MTS(Mobile Trading System) 웹 애플리케이션
> Spring Boot 3.2 + React 18 기반, 네이버 금융 실시간 크롤링

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [아키텍처](#3-아키텍처)
4. [실행 방법](#4-실행-방법)
5. [디렉토리 구조](#5-디렉토리-구조)
6. [데이터베이스 스키마](#6-데이터베이스-스키마)
7. [API 명세](#7-api-명세)
8. [프론트엔드 라우팅 & 페이지](#8-프론트엔드-라우팅--페이지)
9. [핵심 구현 사항](#9-핵심-구현-사항)
10. [외부 데이터 소스](#10-외부-데이터-소스)
11. [환경 변수 & 설정](#11-환경-변수--설정)
12. [알려진 제약사항](#12-알려진-제약사항)
13. [고도화 이력](#13-고도화-이력)

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | StockSimulator |
| 유형 | 모의주식 MTS (증권사 앱 수준) |
| 대상 사용자 | 주식 투자 입문자, 모의 투자 연습자 |
| 데이터 | 네이버 금융 실시간 크롤링 (KOSPI / KOSDAQ) |
| 계좌 | 1인 1계좌, 시드머니 자유 설정 (100만 ~ 5,000만) |
| 인증 | 자체 회원가입 / 카카오 소셜 로그인 |

### 주요 기능

- 실시간 주가 조회 (WebSocket STOMP + 네이버 금융 스케줄러)
- 캔들스틱 차트 (일봉 / 주봉 / 월봉)
- 종목 매수 / 매도 주문
- 관심종목 등록 / 해제
- 시세 랭킹 (급등 / 급락 / 거래량 top 20)
- 포트폴리오 현황 (자산 배분 파이차트, 평가손익)
- 거래내역 (기간 필터, 실현손익 요약)
- 시장 지수 (KOSPI / KOSDAQ)

---

## 2. 기술 스택

### Backend

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 17 |
| 프레임워크 | Spring Boot | 3.2.3 |
| ORM | Spring Data JPA / Hibernate | 6.x |
| DB | MySQL | 9.x |
| 실시간 통신 | Spring WebSocket (STOMP) | - |
| HTML 파싱 | Jsoup | 1.17.2 |
| JSON | Gson | 2.10.1 |
| 빌드 | Maven (mvnw) | - |
| 기타 | Lombok | - |

### Frontend

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | JavaScript (ES2022+) | - |
| 프레임워크 | React | 18.2.0 |
| 라우팅 | React Router DOM | 6.x |
| 차트 | lightweight-charts (TradingView) | 5.1.0 |
| 차트 (파이) | Recharts | 2.12.2 |
| UI 아이콘 | MUI Icons | 5.x |
| WebSocket | @stomp/stompjs + sockjs-client | 7.x |
| 토스트 알림 | react-hot-toast | 2.x |
| CSS | CSS Modules | - |
| 빌드 | Create React App | 5.0.1 |

---

## 3. 아키텍처

```
┌─────────────────────────────────────────────────┐
│                  Browser (React)                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │  Pages   │  │ AppContext│  │ useWebSocket │  │
│  │ (Routes) │  │  (전역상태)│  │  (STOMP)    │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
└───────────────────────┬─────────────────────────┘
          REST API       │        WebSocket (STOMP)
          HTTP/JSON      │        ws://localhost:8090/ws
┌───────────────────────┼─────────────────────────┐
│          Spring Boot  │  (port 8090)             │
│  ┌────────────────────┴──────────────────────┐  │
│  │              Controllers                  │  │
│  └───────────────┬───────────────────────────┘  │
│  ┌───────────────┴───────────────────────────┐  │
│  │  Services / Components (크롤링 포함)        │  │
│  └───────────────┬───────────────────────────┘  │
│  ┌───────────────┴───────────────────────────┐  │
│  │         JPA Repositories                  │  │
│  └───────────────┬───────────────────────────┘  │
└──────────────────┼──────────────────────────────┘
                   │
         ┌─────────┴──────────┐
         │  MySQL (port 3306) │
         │  DB: stocksimulator│
         └────────────────────┘
                   +
         ┌─────────────────────┐
         │  Naver Finance API  │
         │  (크롤링 / Jsoup)    │
         └─────────────────────┘
```

### 실시간 시세 흐름

```
StockPriceScheduler (5초 주기)
  └→ StockDataComponent.getStockChart()  (Naver 크롤링)
       └→ SimpMessagingTemplate.convertAndSend("/topic/price")
            └→ useWebSocket.js (STOMP subscribe)
                 └→ AppContext.realtimePrices (전역 상태)
                      └→ WatchlistCard / StockDetailPage (실시간 반영)
```

---

## 4. 실행 방법

### 사전 요구사항

- Java 17+
- Node.js 18+
- MySQL 8+ (또는 9.x)

### DB 설정

```sql
CREATE DATABASE stocksimulator CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

`application.yml` 환경변수 설정:

```bash
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
```

### 백엔드 실행

```bash
cd back
./mvnw spring-boot:run
# → http://localhost:8090
```

### 프론트엔드 실행

```bash
cd front
npm install
npm start
# → http://localhost:3000
```

### MySQL 서비스 (macOS)

```bash
brew services start mysql
```

---

## 5. 디렉토리 구조

### Backend

```
back/src/main/java/dev/pomyharry/stocksimulator/back/
├── BackApplication.java
├── config/
│   ├── CorsConfig.java              # CORS 전체 허용
│   └── WebSocketConfig.java         # STOMP 엔드포인트 /ws
├── controller/
│   ├── AccountController.java       # 계좌 CRUD
│   ├── CustomerController.java      # 회원가입 / 로그인
│   ├── KakaoController.java         # 카카오 OAuth
│   ├── MarketIndexController.java   # KOSPI / KOSDAQ 지수
│   ├── MarketInfoController.java    # 종목 기본 정보
│   ├── MyStockController.java       # 매수 / 매도
│   ├── SiseRankController.java      # 급등 / 급락 / 거래량 랭킹
│   ├── StockController.java         # 종목 검색
│   ├── StockDataController.java     # 관심종목 시세
│   ├── StockDetailController.java   # 종목 상세 + 캔들
│   ├── PendingOrderController.java  # 지정가 주문 등록/조회/취소
│   ├── TradeHistoryController.java  # 거래내역
│   ├── WatchStockController.java    # 관심종목 관리
│   └── component/
│       ├── CandleDataComponent.java     # Naver fchart OHLCV 크롤링
│       ├── MarketIndexComponent.java    # KOSPI / KOSDAQ 크롤링
│       ├── SiseRankComponent.java       # 급등 / 급락 / 거래량 크롤링
│       └── StockDataComponent.java      # 종목 현재가 크롤링
├── model/
│   ├── dto/                         # 요청 / 응답 DTO
│   └── entity/                      # JPA 엔티티
├── repository/                      # Spring Data JPA
├── scheduler/
│   ├── StockPriceScheduler.java     # 5초 주기 시세 갱신 → STOMP broadcast
│   └── PendingOrderScheduler.java   # 30초 주기 지정가 주문 체결 확인
└── service/                         # 비즈니스 로직 (interface + impl)
```

### Frontend

```
front/src/
├── App.js                           # 라우터 루트
├── App.css                          # 글로벌 레이아웃 (2열 데스크탑)
├── index.css                        # CSS 변수 / 리셋
├── context/
│   └── AppContext.jsx               # 전역 상태 (userId, realtimePrices)
├── hooks/
│   └── useWebSocket.js              # STOMP 연결 / 구독
├── layout/
│   ├── Header/                      # 상단 헤더
│   ├── BottomNav/                   # 모바일 하단 탭 (768px 미만)
│   ├── Sidebar/                     # 데스크탑 좌측 네비 (768px 이상)
│   └── MarketIndex/                 # KOSPI / KOSDAQ 지수 바
├── pages/
│   ├── Home/                        # 홈 (급등 스크롤 + 관심종목)
│   ├── Stocks/                      # 종목검색 + 인기/급등/급락/거래량 탭
│   ├── StockDetail/                 # 캔들차트 + 주문패널
│   ├── Order/                       # 주문 전용 페이지
│   ├── Portfolio/                   # 포트폴리오 (파이차트 + 계좌 생성)
│   ├── History/                     # 거래내역 (기간필터 + 실현손익)
│   ├── My/                          # 내 정보 / 계좌 관리
│   └── PendingOrders/               # 지정가 대기주문 조회 / 취소
└── features/
    └── KakaoLogin/                  # 카카오 OAuth 콜백 처리
```

---

## 6. 데이터베이스 스키마

### 엔티티 관계

```
Customer (1) ──< WatchStock   (N)
Customer (1) ──< Account      (1)   ← 1인 1계좌 제약
Customer (1) ──< MyStock      (N)
Customer (1) ──< TradeHistory (N)
Customer (1) ──< PendingOrder (N)
```

> `stock` 테이블은 더 이상 `watch_stock`, `my_stock`과 FK 관계 없음.
> 종목코드·종목명을 plain VARCHAR로 저장, 검색/가격은 Naver Finance에서 직접 조회.

### 주요 테이블

**customer**

| 컬럼 | 타입 | 설명 |
|------|------|------|
| customer_id | VARCHAR (UUID) | PK |
| name | VARCHAR | 이름 |
| email | VARCHAR | 이메일 (unique) |
| password | VARCHAR | 비밀번호 |

**account**

| 컬럼 | 타입 | 설명 |
|------|------|------|
| account_id | VARCHAR (UUID) | PK |
| account_name | VARCHAR | 계좌명 |
| deposit | BIGINT | 현재 예수금 |
| seed_money | BIGINT | 최초 시드머니 |
| customer_id | VARCHAR | FK → customer |

**my_stock** (보유종목)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| mystock_id | VARCHAR (UUID) | PK |
| stock_code | VARCHAR | 종목코드 (plain, FK 없음) |
| stock_name | VARCHAR | 종목명 (plain) |
| amount | INT | 보유수량 |
| total_buy_price | BIGINT | 총 매입금액 |
| customer_id | VARCHAR | FK → customer |

**watch_stock** (관심종목)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| watch_id | VARCHAR (UUID) | PK |
| stock_code | VARCHAR | 종목코드 (plain, FK 없음) |
| stock_name | VARCHAR | 종목명 (plain) |
| watch_index | BIGINT | 정렬 순서 |
| customer_id | VARCHAR | FK → customer |

**trade_history** (거래내역)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| trade_id | VARCHAR (UUID) | PK |
| trade_type | ENUM(BUY, SELL) | 매수/매도 구분 |
| stock_name | VARCHAR | 종목명 |
| amount | INT | 수량 |
| price_per_stock | BIGINT | 주당 가격 |
| total_price | BIGINT | 총 금액 |
| trade_time | DATETIME | 체결 시각 |
| customer_id | VARCHAR | FK → customer |

---

## 7. API 명세

### 인증 / 회원

| Method | URL | Body | 설명 |
|--------|-----|------|------|
| POST | `/join` | `{name, email, password}` | 회원가입 |
| POST | `/login` | `{email, password}` | 로그인 |
| POST | `/info/customer` | `{id}` | 고객 정보 조회 |
| GET | `/kakaologin` | - | 카카오 로그인 리다이렉트 |
| POST | `/afterlogin` | `{code}` | 카카오 콜백 처리 |

### 계좌

| Method | URL | Body | 설명 |
|--------|-----|------|------|
| POST | `/account` | `{name, deposit, seedMoney, customerId}` | 계좌 생성 |
| POST | `/info/account` | `{customerId}` | 계좌 조회 (보유주식 평가금 포함) |
| PUT | `/info/account` | `{id, deposit, ...}` | 계좌 수정 |
| DELETE | `/info/account` | `{id, customerId}` | 계좌 삭제 |

**계좌 조회 응답 예시**

```json
{
  "id": "uuid",
  "name": "모의투자 계좌",
  "deposit": 8500000,
  "seedMoney": 10000000,
  "sumTotalNowPrice": 1500000
}
```

### 종목

| Method | URL | Body | 설명 |
|--------|-----|------|------|
| POST | `/stocks` | `{name}` | 종목 검색 (KRX 전체 목록 메모리 캐싱 — 전체 KOSPI/KOSDAQ) |
| GET | `/stock-detail/{code}` | `?timeframe=day\|week\|month` | 종목 상세 + 캔들 |
| POST | `/stock-data` | `{id}` | 관심종목 현재가 일괄 조회 |

**종목 상세 응답 예시**

```json
{
  "code": "005930",
  "name": "삼성전자",
  "price": "58,200",
  "change": "▲ 800",
  "changeRate": "+1.39%",
  "volume": "12,345,678",
  "high52w": "88,800",
  "low52w": "49,900",
  "per": "13.52",
  "pbr": "1.14",
  "candles": [
    {"date": "2025-09-23", "open": 57800, "high": 58500, "low": 57600, "close": 58200, "volume": 9876543},
    ...
  ]
}
```

### 관심종목

| Method | URL | Body | 설명 |
|--------|-----|------|------|
| POST | `/stocks/watch` | `{code, name, customerId}` | 관심종목 추가 |
| DELETE | `/stocks/watch` | `{code}` | 관심종목 삭제 |

### 매매

| Method | URL | Body | 설명 |
|--------|-----|------|------|
| POST | `/stocks/buy-stock` | `{customerId, stockCode, name, amount, buyPrice}` | 매수 |
| POST | `/stocks/sell-stock` | `{customerId, stockCode, amount, sellPrice}` | 매도 |
| POST | `/stocks/my-stock` | `{customerId}` | 보유종목 조회 |

### 거래내역

| Method | URL | Body | 설명 |
|--------|-----|------|------|
| POST | `/trade-history` | `{customerId}` | 전체 거래내역 조회 |

### 시세 랭킹

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/sise/rising` | 급등 top 20 |
| GET | `/sise/falling` | 급락 top 20 |
| GET | `/sise/volume` | 거래량 top 20 |

**랭킹 응답 예시**

```json
[
  {
    "code": "013000",
    "name": "세우글로벌",
    "price": "1,478",
    "change": "▲ 341",
    "changeRate": "+29.99%",
    "volume": "8,655,535"
  }
]
```

### 시장 지수

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/market-index` | KOSPI / KOSDAQ 현재 지수 |

### WebSocket

| 프로토콜 | 엔드포인트 | 설명 |
|---------|-----------|------|
| STOMP over SockJS | `ws://localhost:8090/ws` | 연결 |
| Subscribe | `/topic/price` | 5초 주기 전체 종목 시세 broadcast |

**시세 메시지 형식**

```json
{
  "005930": {"price": "58,200", "change": "▲ 800", "changeRate": "+1.39%"},
  "000660": {"price": "183,500", "change": "▼ 1,500", "changeRate": "-0.81%"}
}
```

---

## 8. 프론트엔드 라우팅 & 페이지

| 경로 | 컴포넌트 | 설명 |
|------|---------|------|
| `/` | HomePage | 급등 스크롤 + 관심종목 + 자산 요약 |
| `/stocks` | StocksPage | 종목 검색 + 인기/급등/급락/거래량 탭 |
| `/stock/:code` | StockDetailPage | 캔들차트 + 실시간 시세 + 주문 패널 |
| `/order` | OrderPage | 주문 전용 페이지 |
| `/portfolio` | PortfolioPage | 자산 배분 파이차트 + 보유종목 |
| `/history` | TradeHistoryPage | 거래내역 + 실현손익 |
| `/my` | MyPage | 계좌 관리 + 로그아웃 |
| `/orders/pending` | PendingOrdersPage | 대기주문 조회 / 취소 |
| `/KakaoLogin/:code` | KakaoLogin | 카카오 OAuth 콜백 |

### CSS 변수 (index.css)

```css
--bg-primary:    #0a0e1a   /* 최상위 배경 */
--bg-secondary:  #111827   /* 사이드바 배경 */
--bg-card:       #141927   /* 카드 배경 */
--bg-card-hover: #1a2035   /* 카드 hover */
--border:        #1f2845   /* 구분선 */
--accent:        #4f8ef7   /* 포인트 색상 (파랑) */
--rise:          #ff4757   /* 상승 (빨강, 한국식) */
--fall:          #1e90ff   /* 하락 (파랑, 한국식) */
--text-primary:  #e8eaed
--text-secondary:#c8d0e0
--text-muted:    #8892a4
--header-height: 52px
--sidebar-width: 220px
```

---

## 9. 핵심 구현 사항

### 캔들 데이터 파싱 (CandleDataComponent)

네이버 fchart API는 XML 형식으로 응답하며, 각 캔들은 pipe(`|`) 구분자를 사용:

```
<item data="20260312|58200|59100|57800|58500|9876543" />
         날짜    시가  고가  저가  종가   거래량
```

```java
// Jsoup XML 파서 사용
Document doc = Jsoup.connect(url).parser(Parser.xmlParser()).get();
Elements items = doc.select("item");
String[] parts = item.attr("data").split("\\|");
// YYYYMMDD → YYYY-MM-DD 변환 후 CandleDTO 빌드
```

**URL 패턴**

```
https://fchart.stock.naver.com/sise.nhn
  ?symbol=005930
  &timeframe=day|week|month
  &count=120|52|36
  &requestType=0
```

### 시세 랭킹 파싱 (SiseRankComponent)

네이버 sise 페이지는 EUC-KR 인코딩. Jsoup이 자동 처리.

실제 HTML 구조 (주의: `td.name` 아님, `a.tltle`):

```html
<tr>
  <td><a href="/item/main.naver?code=013000" class="tltle">세우글로벌</a></td>
  <td class="number">1,478</td>              <!-- 현재가 -->
  <td class="number">
    <em class="bu_p bu_pup2">...</em>
    <span class="tah p11 red02">341</span>   <!-- 전일비 숫자 -->
  </td>
  <td class="number">
    <span class="tah p11 red01">+29.99%</span>  <!-- 등락률 -->
  </td>
  <td class="number">8,655,535</td>          <!-- 거래량 -->
</tr>
```

```java
Elements links = doc.select("a.tltle");
for (Element link : links) {
    Element row = link.closest("tr");
    Elements numTds = row.select("td.number");
    // span.tah에서 숫자만 추출
    String changeNum = td.selectFirst("span.tah").text();
}
```

### lightweight-charts v5 주의사항

v4 → v5 API 변경점 (v5.1.0 설치됨):

```javascript
// ❌ v4 방식 (사용 불가)
chart.addCandlestickSeries({ ... })
chart.addHistogramSeries({ ... })

// ✅ v5 방식
import { CandlestickSeries, HistogramSeries, ColorType } from 'lightweight-charts';
chart.addSeries(CandlestickSeries, { ... })
chart.addSeries(HistogramSeries, { priceScaleId: 'vol' })  // 'volume' 예약어 충돌 주의

// 배경색 설정
background: { type: ColorType.Solid, color: '#141927' }
```

### 전역 상태 (AppContext)

```javascript
// context/AppContext.jsx
{
  userId,           // 로그인한 사용자 ID
  userName,         // 이름
  isLoggedIn,       // boolean
  realtimePrices,   // { "005930": { price, change, changeRate }, ... }
  login(id, name),
  logout()
}
```

### 캔들 timeframe 캐싱 (StockDetailPage)

```javascript
const [candleCache, setCandleCache] = useState({});
// 탭 전환 시 캐시 확인 → 없으면 API 요청
const fetchCandles = async (tf) => {
  if (candleCache[tf]) { setTimeframe(tf); return; }
  const res = await fetch(`/stock-detail/${code}?timeframe=${tf}`);
  setCandleCache(prev => ({ ...prev, [tf]: data.candles }));
};
```

---

## 10. 외부 데이터 소스

### 네이버 금융 크롤링 엔드포인트 (가격 / 차트 데이터)

| 데이터 | URL |
|--------|-----|
| 현재가 / 등락 | `https://finance.naver.com/item/sise.naver?code={CODE}` |
| 종목 상세 (PER, PBR, 52주 고저) | `https://finance.naver.com/item/main.naver?code={CODE}` |
| 캔들 OHLCV | `https://fchart.stock.naver.com/sise.nhn?symbol={CODE}&timeframe=day&count=120&requestType=0` |
| 급등 | `https://finance.naver.com/sise/sise_rise.naver` |
| 급락 | `https://finance.naver.com/sise/sise_fall.naver` |
| 거래량 | `https://finance.naver.com/sise/sise_quant.naver` |
| KOSPI / KOSDAQ | `https://finance.naver.com/sise/sise_index.naver?code=KOSPI` |

### KRX 데이터 포털 (종목 검색 목록)

| 데이터 | 방식 | URL |
|--------|------|-----|
| 전체 상장 종목 목록 | POST (form) | `https://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd` |

- 애플리케이션 **시작 시 1회 로드** → `KrxStockListService`가 메모리에 캐싱
- 이후 `POST /stocks` 검색은 외부 호출 없이 메모리 내 필터링
- KRX 로드 실패 시 주요 종목 90개 **FALLBACK 목록**으로 자동 대체
- 네이버 금융 검색 API는 서버사이드 요청 시 모두 차단(403/404)되어 사용 불가

```
POST bld=dbms/MDC/STAT/standard/MDCSTAT01901&locale=ko_KR&mktId=ALL
응답: { "OutBlock_1": [{ "ISU_SRT_CD": "005930", "ISU_ABBRV": "삼성전자", "SECUGRP_NM": "주권", ... }] }
필터: SECUGRP_NM == "주권" (ETF·수익증권 제외)
```

**공통 요청 헤더**

```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ...
Referer: https://finance.naver.com/
Timeout: 8000ms
```

---

## 11. 환경 변수 & 설정

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/stocksimulator?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
  jpa:
    hibernate:
      ddl-auto: update   # 개발: update / 운영: validate

server:
  port: 8090
```

### 프론트엔드 BASE_URL

현재 모든 페이지에 하드코딩:

```javascript
const BASE_URL = 'http://localhost:8090';
```

> 배포 시 `.env` 파일로 분리 필요:
> `REACT_APP_API_URL=https://api.yourdomain.com`

### 카카오 로그인 설정

`KakaoController.java` 내부 하드코딩된 REST API Key 및 Redirect URI 수정 필요.

---

## 12. 알려진 제약사항

| 항목 | 내용 |
|------|------|
| 계좌 | 1인 1계좌만 허용 |
| 종목 검색 | KRX 전체 목록 시작 시 로드, 메모리 검색 — 전체 KOSPI/KOSDAQ 검색 가능. KRX 불가 시 주요 90개 FALLBACK |
| 크롤링 | 네이버 금융 HTML 구조 변경 시 파싱 중단 가능 |
| 실시간 시세 | 스케줄러 5초 주기 → 진짜 실시간 아님 |
| 주문 방식 | 시장가 + 지정가(지정가매수/익절/손절) 지원 |
| 인증 | JWT 미사용, userId를 localStorage에 저장 |
| CORS | 전체 허용 (`origins = "*"`) → 운영 시 도메인 제한 필요 |
| 비밀번호 | 암호화 미적용 (BCrypt 등 적용 권장) |

---

## 부록: 주요 파일 변경 이력

| 파일 | 주요 변경 |
|------|---------|
| `CandleDataComponent.java` | fchart XML 파싱 (pipe 구분자), timeframe 파라미터 지원 |
| `SiseRankComponent.java` | `a.tltle` 셀렉터, `span.tah` 숫자 추출, EUC-KR 자동 처리 |
| `StockDetailController.java` | `?timeframe=` 쿼리 파라미터 추가 |
| `AccountController.java` | 응답에 `seedMoney` 필드 추가 |
| `CandleChart.jsx` | lightweight-charts v5 API 대응 |
| `StockDetailPage.jsx` | 일봉/주봉/월봉 탭 + 캔들 캐싱 |
| `PortfolioPage.jsx` | 계좌 생성 인라인 폼 + Recharts PieChart |
| `StocksPage.jsx` | 인기/급등/급락/거래량 탭 |
| `TradeHistoryPage.jsx` | 기간 필터 + 실현손익 요약 |
| `HomePage.jsx` | 급등 종목 수평 스크롤 섹션 |

---

## 13. 고도화 이력

### 버그 수정 및 안정화

| 항목 | 내용 |
|------|------|
| 거래시간 예외 계정 | `dohee789@naver.com` 24시간 거래 허용 (`MARKET_HOURS_EXEMPT` Set) |
| 매도 평균단가 버그 | `sellStock` 시 `totalBuyPrice` 차감을 매도금액 → 평균매입가×수량으로 수정 |
| 0원 매수 방지 | `buyStock` 시 `buyPrice <= 0` 검증 추가 |
| 거래내역 날짜 오류 | `application.yml`에 `write-dates-as-timestamps: false` 추가 → LocalDateTime ISO 형식 직렬화 |
| 비인기 종목 상세 오류 | DB 없는 종목도 Naver Finance에서 직접 조회하도록 fallback 처리 |
| 거래내역 Lazy Loading | `TradeHistoryServiceImpl.findByCustomerId()`에 `@Transactional(readOnly = true)` 추가 |
| 매수 현재가 미조회 | OrderPage `/stocks/watch-list` → `/stock-data` 변경으로 현재가 정상 조회 |

### API 추가

| Method | URL | Body | 설명 |
|--------|-----|------|------|
| POST | `/account/reset` | `{customerId}` | 계좌 초기화 (보유종목·거래내역 삭제, 예수금 → 시드머니) |
| POST | `/pending-order` | `{customerId, stockCode, orderType, conditionType, targetPrice, amount}` | 지정가 주문 등록 |
| POST | `/pending-order/list` | `{customerId}` | 지정가 주문 목록 조회 |
| POST | `/pending-order/cancel` | `{orderId, customerId}` | 지정가 주문 취소 |

**계좌 초기화 처리 흐름:**
```
POST /account/reset
  → AccountServiceImpl.resetAccount(customerId)
  → myStockRepository.deleteByCustomerId(customerId)
  → tradeHistoryRepository.deleteAllByCustomerId(customerId)
  → account.setDeposit(account.getSeedMoney())
  → accountRepository.save(account)
```

### 지정가 주문 시스템

#### 지원 주문 유형
| 유형 | orderType | conditionType | 체결 조건 |
|------|-----------|---------------|---------|
| 지정가 매수 | BUY | PRICE_BELOW | 현재가 ≤ 목표가 |
| 익절 매도 | SELL | PRICE_ABOVE | 현재가 ≥ 목표가 |
| 손절 매도 | SELL | PRICE_BELOW | 현재가 ≤ 목표가 |

#### 엔티티 — `pending_order`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| order_id | VARCHAR (UUID) | PK |
| stock_code | VARCHAR | 종목코드 |
| stock_name | VARCHAR | 종목명 |
| order_type | VARCHAR | BUY \| SELL |
| condition_type | VARCHAR | PRICE_BELOW \| PRICE_ABOVE |
| target_price | BIGINT | 목표가 |
| amount | INT | 수량 |
| status | VARCHAR | PENDING \| EXECUTED \| CANCELLED |
| created_at | DATETIME | 등록 시각 |
| executed_at | DATETIME | 체결 시각 (nullable) |
| customer_id | VARCHAR | FK → customer |

#### 자동 체결 스케줄러
- `PendingOrderScheduler`: 30초마다 전체 PENDING 주문 확인 → 조건 충족 시 자동 체결
- `JOIN FETCH p.customer`로 customer 즉시 로딩 (LazyInitializationException 방지)
- `checkAndExecuteAll()` 비트랜잭션 — 각 매수/매도는 독립 트랜잭션으로 처리
- 체결 시 TradeHistory 자동 기록

### 프론트엔드 개선

#### OrderPage 전면 개편
- `/stocks/watch-list` → `/stock-data` 변경으로 관심종목에 현재가·등락률 표시
- 종목 목록: 카드 형태 (종목명 + 코드 + 현재가 + 등락률)
- 예수금 바: 주문 가능 예수금 / 보유 종목 수 표시
- 시장가 / 지정가 토글, 목표가 입력 UI (현재가 대비 % 표시)
- 지정가 매도 조건 선택 (익절 / 손절)
- 비율 버튼: 25% / 50% / 75% / 최대

#### MyPage 개선
- 투자 성과 요약 카드 (총자산, 시드대비수익률, 평가손익, 보유종목, 평가수익률)
- 계좌 초기화 버튼 (확인 팝업 후 실행)

#### HomePage 개선
- 급등/급락 탭 토글: 섹션 우측 탭으로 전환, 급락은 lazy load

#### 신규 페이지 — `/orders/pending`
- 대기주문 조회 (PENDING / EXECUTED / CANCELLED / ALL 필터)
- PENDING 주문 취소 버튼

### 코드 정리
- 모든 페이지 `BASE_URL` 하드코딩 → `config/api.js` 중앙화 (`API_BASE_URL`)
- `Header.jsx`: 히스토리 아이콘(→ `/history`), 알림 아이콘(toast 안내) 기능 추가
- `StockDetailPage.jsx`: 주문 확인 모달, 보유현황 표시, 매도 시 빠른 비율 버튼 추가
- `TradeHistoryPage.jsx`: 각 거래내역 클릭 시 해당 종목 상세 페이지로 이동

### 종목 DB 의존성 제거 — 전 종목 지원

- `watch_stock`, `my_stock`의 `stock_code` FK 제거 → `stock_code` + `stock_name` plain VARCHAR로 변경
- `StockDataComponent.getStockChart(WatchStock)` → `getStockChart(String code, String name)` 시그니처 변경
- `CandleDataComponent.enrichStockDetail()`에 종목명 자동 스크래핑 추가 (Naver 페이지 타이틀)
- `StockDetailController`, `StockPriceScheduler`, `PendingOrderServiceImpl`, `MyStockServiceImpl`에서 `StockRepository` 의존성 완전 제거
- `StockController` 검색: DB 75개 → **KRX 데이터 포털** 전환 → 전체 KOSPI/KOSDAQ 검색 가능
  - Naver 검색 API는 서버사이드 요청을 차단하여 사용 불가 (ac.finance.naver.com DNS 불가, searchList.naver 404, finance.naver.com/search 403 등)
  - `KrxStockListService`: 앱 시작 시 KRX에서 전체 종목 로드 → 이후 메모리 검색 (외부 호출 없음)
  - KRX 로드 실패 시 FALLBACK 목록(주요 90개) 사용
- Frontend: 관심종목 추가 시 `name` 필드 포함, 매수 시 `name` 필드 포함
- DB 마이그레이션: `watch_stock`, `my_stock`의 stock FK 수동 제거 필요 (`ALTER TABLE ... DROP FOREIGN KEY`)
