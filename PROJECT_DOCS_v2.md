# 모의주식 MTS

> Spring Boot 3.2 + React 18 기반의 모의주식 트레이딩 웹 앱.
> (계속 고도화중 !!!)

---

## 목차

1. [프로젝트 개요](#-프로젝트-개요)
2. [기술 스택](#-기술-스택)
3. [시스템 아키텍처](#-시스템-아키텍처)
4. [실행 방법](#-실행-방법)
5. [디렉토리 구조](#-디렉토리-구조)
6. [데이터베이스 스키마](#-데이터베이스-스키마)
7. [API 명세](#-api-명세)
8. [프론트엔드 라우팅 & UI](#-프론트엔드)
9. [핵심 구현 트러블슈팅](#-핵심-구현-트러블슈팅)
10. [외부 데이터 소스](#-외부-데이터-소스)
11. [알려진 제약사항](#-알려진-제약사항)

---

## 📌 프로젝트 개요

- **프로젝트명:** StockSimulator
- **유형:** 모의주식 MTS (증권사 앱 수준)
- **대상 사용자:** 주식 투자 입문자, 모의 투자 연습자
- **데이터:** Yahoo Finance v8 chart API (가격/차트) + KRX 데이터 포털 (종목 목록/지수 보조)
- **계좌:** 1인 1계좌, 시드머니 자유 설정 (100만 ~ 5,000만)
- **인증:** 자체 회원가입 / 카카오 소셜 로그인

### 주요 기능

- **실시간 주가 조회** — WebSocket STOMP + Yahoo Finance 스케줄러 (5초 주기)
- **캔들스틱 차트** — 일봉 / 주봉 / 월봉
- **종목 매수 / 매도** — 현재가 시장가 주문 / 예약 지정가 주문
- **관심종목** — 등록 / 해제
- **시세 랭킹** — 급등 / 급락 / 거래량 top 20 (KOSPI·KOSDAQ 주요 종목 대상)
- **포트폴리오** — 자산 배분 파이차트, 평가손익 현황
- **거래내역** — 기간 필터, 실현손익 요약
- **시장 지수** — KOSPI / KOSDAQ 실시간 지수
- **선물 거래** — 레버리지 기반 지수·원자재 선물 모의투자 (롱/숏, 강제청산)

---

## 🛠 기술 스택

### Backend

| 분류 | 기술 | 버전 |
| --- | --- | --- |
| 언어 | Java | 17 |
| 프레임워크 | Spring Boot | 3.2.3 |
| ORM | Spring Data JPA / Hibernate | 6.x |
| DB | MySQL | 9.x |
| 실시간 통신 | Spring WebSocket (STOMP) | - |
| JSON | Gson | 2.10.1 |
| 빌드 | Maven (mvnw) | - |
| 기타 | Lombok | - |

### Frontend

| 분류 | 기술 | 버전 |
| --- | --- | --- |
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

## 🏗 시스템 아키텍처

```
Browser (React)
  ├── Pages (Routes)
  ├── AppContext (전역 상태)
  └── useWebSocket (STOMP)
        │
        ├── REST API (HTTP/JSON)
        └── WebSocket STOMP  ←→  ws://localhost:8090/ws

Spring Boot (port 8090)
  ├── Controllers
  ├── Services / Components
  └── JPA Repositories
        │
        ├── MySQL (port 3306)              ←  DB: stocksimulator
        ├── Yahoo Finance v8 chart API     ←  주가·캔들·지수·선물 (1차)
        ├── Yahoo Finance v7 quote API     ←  시세 랭킹 배치 조회
        └── KRX 데이터 포털                ←  전체 종목 목록 + KOSPI200 지수 보조
```

### 실시간 시세 데이터 흐름

```
StockPriceScheduler (5초 주기)
  └→ StockDataComponent.getStockChart()   (Yahoo Finance v8 chart API)
       └→ SimpMessagingTemplate.convertAndSend("/topic/price")
            └→ useWebSocket.js (STOMP subscribe)
                 └→ AppContext.realtimePrices (전역 상태)
                      └→ WatchlistCard / StockDetailPage (실시간 반영)
```

### 선물 강제청산 흐름

```
FuturesLiquidationScheduler (60초 주기)
  └→ FuturesService.checkLiquidations()
       └→ FuturesDataComponent.getPrice()  (Yahoo Finance)
            └→ pnl <= -marginAmount → status = LIQUIDATED, 예수금 반환 없음
```

---

## ▶️ 실행 방법

### DB 설정

```sql
CREATE DATABASE stocksimulator CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
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

---

## 📁 디렉토리 구조

### Backend

```
back/src/main/java/dev/pomyharry/stocksimulator/back/
├── BackApplication.java
├── config/
│   ├── CorsConfig.java                  # CORS 전체 허용
│   └── WebSocketConfig.java             # STOMP 엔드포인트 /ws
├── controller/
│   ├── AccountController.java           # 계좌 CRUD
│   ├── CustomerController.java          # 회원가입 / 로그인
│   ├── FuturesController.java           # 선물 목록 / 현재가 / 포지션 관리
│   ├── KakaoController.java             # 카카오 OAuth
│   ├── MarketIndexController.java       # KOSPI / KOSDAQ 지수
│   ├── MyStockController.java           # 매수 / 매도
│   ├── SiseRankController.java          # 급등 / 급락 / 거래량 랭킹
│   ├── StockController.java             # 종목 검색 (KrxStockListService 사용)
│   ├── StockDataController.java         # 관심종목 시세
│   ├── StockDetailController.java       # 종목 상세 + 캔들
│   ├── PendingOrderController.java      # 지정가 주문 등록 / 조회 / 취소
│   ├── TradeHistoryController.java      # 거래내역
│   ├── WatchStockController.java        # 관심종목 관리
│   └── component/
│       ├── CandleDataComponent.java     # Yahoo Finance v8 OHLCV + 종목 상세
│       ├── FuturesDataComponent.java    # Yahoo Finance v8 선물 현재가 (KRX 보조)
│       ├── MarketIndexComponent.java    # Yahoo Finance ^KS11 / ^KQ11
│       ├── SiseRankComponent.java       # Yahoo Finance v7 배치 (주요 종목 랭킹)
│       └── StockDataComponent.java      # Yahoo Finance v8 종목 현재가
├── model/
│   ├── dto/
│   │   ├── CandleDTO.java
│   │   ├── FuturesItemDTO.java          # 선물 종목 정보 + 현재가
│   │   ├── FuturesOrderDTO.java         # 선물 주문 요청
│   │   ├── FuturesPositionDTO.java      # 선물 포지션 응답
│   │   ├── MarketIndexDTO.java
│   │   ├── SiseRankDTO.java
│   │   ├── StockDetailDTO.java
│   │   └── TradeHistoryDTO.java
│   └── entity/
│       ├── Account.java
│       ├── Customer.java
│       ├── FuturesPosition.java         # 선물 포지션 엔티티
│       ├── Kakao.java
│       ├── MyStock.java
│       ├── Stock.java
│       ├── TradeHistory.java
│       └── WatchStock.java
├── repository/
│   ├── FuturesPositionRepository.java
│   └── ...
├── scheduler/
│   ├── FuturesLiquidationScheduler.java # 60초 주기 강제청산 확인
│   ├── PendingOrderScheduler.java       # 30초 주기 지정가 주문 체결 확인
│   └── StockPriceScheduler.java        # 5초 주기 시세 갱신 → STOMP broadcast
└── service/
    ├── FuturesService.java / FuturesServiceImpl.java
    ├── KrxStockListService.java         # 앱 시작 시 KRX 전체 종목 로드 → 메모리 캐싱
    └── ...                              # 비즈니스 로직 (interface + impl)
```

### Frontend

```
front/src/
├── App.js                               # 라우터 루트
├── App.css                              # 글로벌 레이아웃 (2열 데스크탑)
├── index.css                            # CSS 변수 / 리셋
├── context/
│   └── AppContext.jsx                   # 전역 상태 (userId, realtimePrices)
├── hooks/
│   └── useWebSocket.js                  # STOMP 연결 / 구독
├── layout/
│   ├── Header/
│   ├── BottomNav/                       # 모바일 하단 탭 (홈/시세/선물/주문/내주식/MY)
│   ├── Sidebar/                         # 데스크탑 좌측 사이드바 (선물 포함)
│   └── MarketIndex/                     # KOSPI / KOSDAQ 지수 바
├── pages/
│   ├── Futures/
│   │   ├── FuturesPage.jsx              # 선물 목록 (지수 / 원자재 탭, 30초 갱신)
│   │   ├── FuturesDetailPage.jsx        # 포지션 오픈 (롱/숏, 레버리지, 증거금)
│   │   └── FuturesPositionsPage.jsx     # 보유 포지션 / 청산 내역
│   ├── Home/                            # 홈 (급등 스크롤 + 관심종목)
│   ├── Stocks/                          # 종목검색 + 인기/급등/급락/거래량 탭
│   ├── StockDetail/                     # 캔들차트 + 주문패널
│   ├── Order/                           # 주문 전용 페이지
│   ├── Portfolio/                       # 포트폴리오 (파이차트 + 계좌 생성)
│   ├── History/                         # 거래내역 (기간필터 + 실현손익)
│   ├── My/                              # 내 정보 / 계좌 관리
│   └── PendingOrders/                   # 지정가 대기주문 조회 / 취소
└── features/
    └── KakaoLogin/                      # 카카오 OAuth 콜백 처리
```

---

## 🗄 데이터베이스 스키마

### 엔티티 관계

```
Customer (1) ──< WatchStock      (N)
Customer (1) ──  Account         (1)   ← 1인 1계좌 제약
Customer (1) ──< MyStock         (N)
Customer (1) ──< TradeHistory    (N)
Customer (1) ──< PendingOrder    (N)
Customer (1) ──< FuturesPosition (N)
```

> `stock` 테이블은 `watch_stock`, `my_stock`과 FK 관계 없음. 종목코드·종목명을 plain VARCHAR로 저장, 검색은 KRX에서, 가격/차트는 Yahoo Finance에서 직접 조회.

### customer

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| customer_id | VARCHAR (UUID) | PK |
| name | VARCHAR | 이름 |
| email | VARCHAR | 이메일 (unique) |
| password | VARCHAR | 비밀번호 |

### account

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| account_id | VARCHAR (UUID) | PK |
| account_name | VARCHAR | 계좌명 |
| deposit | BIGINT | 현재 예수금 |
| seed_money | BIGINT | 최초 시드머니 |
| customer_id | VARCHAR | FK → customer |

### watch_stock (관심종목)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| watch_id | VARCHAR (UUID) | PK |
| stock_code | VARCHAR | 종목코드 (plain, FK 없음) |
| stock_name | VARCHAR | 종목명 (plain) |
| watch_index | BIGINT | 정렬 순서 |
| customer_id | VARCHAR | FK → customer |

### my_stock (보유종목)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| my_stock_id | VARCHAR (UUID) | PK |
| stock_code | VARCHAR | 종목코드 (plain, FK 없음) |
| stock_name | VARCHAR | 종목명 (plain) |
| amount | INT | 보유수량 |
| total_buy_price | BIGINT | 총 매입금액 |
| now_price | BIGINT | 현재가 (스케줄러 갱신) |
| customer_id | VARCHAR | FK → customer |

### trade_history (거래내역)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| trade_id | VARCHAR (UUID) | PK |
| trade_type | ENUM(BUY, SELL) | 매수/매도 구분 |
| stock_name | VARCHAR | 종목명 |
| amount | INT | 수량 |
| price_per_stock | BIGINT | 주당 가격 |
| total_price | BIGINT | 총 금액 |
| trade_time | DATETIME | 체결 시각 |
| customer_id | VARCHAR | FK → customer |

### pending_order (지정가 주문)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
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

### futures_position (선물 포지션)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | BIGINT | PK (auto increment) |
| customer_id | VARCHAR | FK → customer |
| futures_code | VARCHAR | 선물 코드 (KOSPI200 / NASDAQ100 / SP500 / GOLD / OIL_WTI) |
| futures_name | VARCHAR | 선물 종목명 |
| direction | ENUM(LONG, SHORT) | 방향 |
| margin_amount | BIGINT | 증거금 (KRW) |
| leverage | INT | 레버리지 배율 (2 / 5 / 10 / 20) |
| entry_price | DOUBLE | 진입 시점 현재가 (native currency) |
| status | ENUM(OPEN, CLOSED, LIQUIDATED) | 포지션 상태 |
| opened_at | DATETIME | 포지션 오픈 시각 |
| closed_at | DATETIME | 청산 시각 (nullable) |
| realized_pnl | BIGINT | 실현 손익 KRW (청산 후 확정) |

---

## 📡 API 명세

### 인증 / 회원

| Method | URL | Body | 설명 |
| --- | --- | --- | --- |
| POST | /join | {name, email, password} | 회원가입 |
| POST | /login | {email, password} | 로그인 |
| POST | /info/customer | {id} | 고객 정보 조회 |
| GET | /kakaologin | - | 카카오 로그인 리다이렉트 |
| POST | /afterlogin | {code} | 카카오 콜백 처리 |

### 계좌

| Method | URL | Body | 설명 |
| --- | --- | --- | --- |
| POST | /account | {name, deposit, seedMoney, customerId} | 계좌 생성 |
| POST | /info/account | {customerId} | 계좌 조회 (보유주식 평가금 포함) |
| POST | /account/reset | {customerId} | 계좌 초기화 (보유종목·거래내역 삭제, 예수금 → 시드머니) |
| PUT | /info/account | {id, deposit, ...} | 계좌 수정 |
| DELETE | /info/account | {id, customerId} | 계좌 삭제 |

### 종목

| Method | URL | Body | 설명 |
| --- | --- | --- | --- |
| POST | /stocks | {name} | 종목 검색 (KRX 전체 목록 메모리 검색) |
| GET | /stock-detail/{code} | ?timeframe=day\|week\|month | 종목 상세 + 캔들 |
| POST | /stock-data | {id} | 관심종목 현재가 일괄 조회 |

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
    {"date": "2025-09-23", "open": 57800, "high": 58500, "low": 57600, "close": 58200, "volume": 9876543}
  ]
}
```

### 관심종목

| Method | URL | Body | 설명 |
| --- | --- | --- | --- |
| POST | /stocks/watch | {code, customerId} | 관심종목 추가 |
| DELETE | /stocks/watch | {code} | 관심종목 삭제 |

### 매매

| Method | URL | Body | 설명 |
| --- | --- | --- | --- |
| POST | /stocks/buy-stock | {customerId, stockCode, amount, buyPrice} | 매수 |
| POST | /stocks/sell-stock | {customerId, stockCode, amount, sellPrice} | 매도 |
| POST | /stocks/my-stock | {customerId} | 보유종목 조회 |
| POST | /pending-order | {customerId, stockCode, orderType, conditionType, targetPrice, amount} | 지정가 주문 등록 |
| POST | /pending-order/cancel | {orderId, customerId} | 지정가 주문 취소 |

### 거래내역

| Method | URL | Body | 설명 |
| --- | --- | --- | --- |
| POST | /trade-history | {customerId} | 전체 거래내역 조회 |
| POST | /pending-order/list | {customerId} | 지정가 주문 목록 조회 |

### 시세 랭킹 / 시장 지수

| Method | URL | 설명 |
| --- | --- | --- |
| GET | /sise/rising | 급등 top 20 |
| GET | /sise/falling | 급락 top 20 |
| GET | /sise/volume | 거래량 top 20 |
| GET | /market-index | KOSPI / KOSDAQ 현재 지수 |

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

### 선물

| Method | URL | Body | 설명 |
| --- | --- | --- | --- |
| GET | /futures | - | 지원 선물 5종 + 현재가 전체 조회 |
| GET | /futures/{code} | - | 특정 선물 현재가 조회 |
| POST | /futures/open | {customerId, futuresCode, direction, marginAmount, leverage} | 포지션 오픈 |
| POST | /futures/close | {positionId, customerId} | 포지션 청산 |
| GET | /futures/positions/{customerId} | - | 고객 포지션 목록 (OPEN + CLOSED + LIQUIDATED) |

**지원 선물 종목**

| code | 종목명 | 카테고리 | 통화 | Yahoo 심볼 |
| --- | --- | --- | --- | --- |
| KOSPI200 | 코스피200 선물 | INDEX | KRW | ^KS200 (KRX 폴백) |
| NASDAQ100 | 나스닥100 선물 | INDEX | USD | NQ=F |
| SP500 | S&P500 선물 | INDEX | USD | ES=F |
| GOLD | 금 선물 | COMMODITY | USD | GC=F |
| OIL_WTI | WTI원유 선물 | COMMODITY | USD | CL=F |

**포지션 오픈 응답 예시**

```json
{
  "id": 42,
  "futuresCode": "NASDAQ100",
  "futuresName": "나스닥100 선물",
  "direction": "LONG",
  "marginAmount": 500000,
  "leverage": 10,
  "entryPrice": 19500.0,
  "currentPrice": 19650.0,
  "currency": "USD",
  "unrealizedPnl": 38461,
  "unrealizedPnlRate": 7.69,
  "status": "OPEN",
  "openedAt": "2026-05-28T10:00:00"
}
```

**선물 P&L 계산 방식 (CFD 모델)**

```
평가손익(KRW) = 증거금 × 레버리지 × (현재가 - 진입가) / 진입가 × 방향
방향: LONG = +1 / SHORT = -1

예) 증거금 50만원, 10x 레버리지, LONG, 가격 +1% 상승
  → 손익 = 500,000 × 10 × 0.01 × 1 = +50,000 KRW

강제청산 조건: 평가손익 ≤ -증거금 (증거금 전액 손실)
청산 시 예수금 반환 없음 (LIQUIDATED 상태)
```

### WebSocket

| 항목 | 내용 |
| --- | --- |
| 프로토콜 | STOMP over SockJS |
| 연결 엔드포인트 | ws://localhost:8090/ws |
| 구독 토픽 | /topic/price |
| 발행 주기 | 5초 |

**시세 메시지 형식**

```json
{
  "005930": {"price": "58,200", "change": "▲ 800", "changeRate": "+1.39%"},
  "000660": {"price": "183,500", "change": "▼ 1,500", "changeRate": "-0.81%"}
}
```

---

## 🖥 프론트엔드

### 페이지 라우팅

| 경로 | 페이지 | 설명 |
| --- | --- | --- |
| / | HomePage | 급등 스크롤 + 관심종목 + 자산 요약 |
| /stocks | StocksPage | 종목 검색 + 인기/급등/급락/거래량 탭 |
| /stock/:code | StockDetailPage | 캔들차트 + 실시간 시세 + 주문 패널 |
| /order | OrderPage | 주문 전용 페이지 |
| /portfolio | PortfolioPage | 자산 배분 파이차트 + 보유종목 |
| /history | TradeHistoryPage | 거래내역 + 실현손익 |
| /my | MyPage | 계좌 관리 + 로그아웃 |
| /futures | FuturesPage | 선물 목록 (지수/원자재 탭, 30초 자동갱신) |
| /futures/:code | FuturesDetailPage | 포지션 오픈 (롱/숏, 레버리지, 증거금) |
| /futures/positions | FuturesPositionsPage | 보유 포지션 / 청산 내역 |
| /orders/pending | PendingOrdersPage | 지정가 대기주문 조회 / 취소 |
| /KakaoLogin/:code | KakaoLogin | 카카오 OAuth 콜백 |

### UI

- 홈
  - 로그인

    ![스크린샷 2026-04-14 오후 6.50.21.png](attachment:0f2050ab-7172-485d-ab00-207473164c19:스크린샷_2026-04-14_오후_6.50.21.png)

  - 회원가입

    ![스크린샷 2026-04-14 오후 6.49.56.png](attachment:dd91f5c4-0154-4b63-8149-b8f367e40d78:스크린샷_2026-04-14_오후_6.49.56.png)

  - 메인

    ![스크린샷 2026-04-10 오후 8.18.16.png](attachment:89690288-ea8e-4832-90d6-ca1de64af8cc:스크린샷_2026-04-10_오후_8.18.16.png)

  - 검색

    ![스크린샷 2026-04-14 오후 6.24.51.png](attachment:10b47c63-ada4-4a85-9223-4d7699bc69b3:스크린샷_2026-04-14_오후_6.24.51.png)

- 종목 상세

  ![스크린샷 2026-04-07 오후 9.04.30.png](attachment:da539421-0273-41f7-8fbc-3db87e9532d9:스크린샷_2026-04-07_오후_9.04.30.png)

- 시세 (인기/급등/급락/거래량)
  - 인기

    ![스크린샷 2026-04-07 오후 9.05.47.png](attachment:7659ed77-553f-4322-a45c-c71e3d1e405f:스크린샷_2026-04-07_오후_9.05.47.png)

  - 급등

    ![스크린샷 2026-04-07 오후 9.06.28.png](attachment:352eaeee-b98c-4b24-b52c-5052c791e4e2:스크린샷_2026-04-07_오후_9.06.28.png)

  - 급락

    ![스크린샷 2026-04-07 오후 9.06.58.png](attachment:ac0225d9-2846-4649-8534-8dd7898cd200:스크린샷_2026-04-07_오후_9.06.58.png)

  - 거래량

    ![image.png](attachment:ac78d051-6e10-42c3-bbd1-8ef80d84fd1a:image.png)

- 주문 (매수/매도)
  - 매수 (시장가 매수)

    ![스크린샷 2026-04-10 오후 8.20.14.png](attachment:f7cc4833-db80-4ec8-8626-c95407f8b445:스크린샷_2026-04-10_오후_8.20.14.png)

  - 매도 (지정가 매도)

    ![스크린샷 2026-04-10 오후 8.24.47.png](attachment:6283aa08-b77e-485c-abeb-3d7ed7afd453:스크린샷_2026-04-10_오후_8.24.47.png)

  - 대기 주문

    ![스크린샷 2026-04-10 오후 8.26.20.png](attachment:c12cd92a-0b29-4229-af5f-c517c55c9519:스크린샷_2026-04-10_오후_8.26.20.png)

- 마이페이지 (내주식/거래내역/내계좌)
  - 내주식

    ![스크린샷 2026-04-07 오후 9.20.01.png](attachment:5ebe5553-00a9-4d61-8396-113755f84bdf:스크린샷_2026-04-07_오후_9.20.01.png)

  - 거래내역

    ![스크린샷 2026-04-09 오후 8.30.24.png](attachment:bd6fb408-18a7-400a-b2e5-bcb307c2351a:스크린샷_2026-04-09_오후_8.30.24.png)

  - 내계좌
    - 계좌 생성

      ![스크린샷 2026-04-14 오후 7.30.59.png](attachment:be12f3c1-6257-43fc-818e-615f5eb90466:스크린샷_2026-04-14_오후_7.30.59.png)

    - 내 계좌

      ![스크린샷 2026-04-10 오후 8.19.31.png](attachment:aef33c83-bd26-4629-817e-b207fc61c81d:스크린샷_2026-04-10_오후_8.19.31.png)

---

## 🔧 핵심 구현 트러블슈팅

### 1. Yahoo Finance v8 chart API — 주가 / 캔들 / 지수

모든 주가·캔들·시장 지수·선물 데이터는 Yahoo Finance v8 chart API를 단일 소스로 사용한다.

```
GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval={interval}&range={range}
```

**한국 주식 심볼 규칙**

| 시장 | 심볼 형식 | 예시 |
| --- | --- | --- |
| KOSPI | {종목코드}.KS | 005930.KS |
| KOSDAQ | {종목코드}.KQ | 247540.KQ |
| KOSPI 지수 | ^KS11 | - |
| KOSDAQ 지수 | ^KQ11 | - |

**응답 파싱 핵심 필드**

```java
JsonObject meta = result[0].meta;

// 현재가 / 전일 종가
double price    = meta.get("regularMarketPrice").getAsDouble();
// 선물(FUTURE)은 chartPreviousClose, 주식/지수는 previousClose 사용
double prevClose = meta.has("chartPreviousClose") ? meta.get("chartPreviousClose").getAsDouble()
                 : meta.get("previousClose").getAsDouble();

// 종목명
String name = meta.get("shortName").getAsString();

// 거래량 / 52주 고저
long volume    = meta.get("regularMarketVolume").getAsLong();
double high52w = meta.get("fiftyTwoWeekHigh").getAsDouble();
double low52w  = meta.get("fiftyTwoWeekLow").getAsDouble();
```

**캔들 OHLCV 파싱**

```java
// timestamp: Unix epoch seconds → KST LocalDate
JsonArray timestamps = result[0].timestamp;
JsonObject quote     = result[0].indicators.quote[0];
// quote.open / high / low / close / volume (배열, null 포함 가능)

long ts = timestamps.get(i).getAsLong();
String date = Instant.ofEpochSecond(ts).atZone(ZoneId.of("Asia/Seoul"))
                     .toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
```

**timeframe별 파라미터**

| timeframe | interval | range |
| --- | --- | --- |
| 일봉 (day) | 1d | 6mo |
| 주봉 (week) | 1wk | 2y |
| 월봉 (month) | 1mo | 5y |

**PER / PBR — Yahoo Finance v10 quoteSummary**

```
GET https://query1.finance.yahoo.com/v10/finance/quoteSummary/{symbol}?modules=summaryDetail
응답: quoteSummary.result[0].summaryDetail.trailingPE.fmt
                                            .priceToBook.fmt
```

---

### 2. 시세 랭킹 — Yahoo Finance v7 배치 quote

Naver Finance sise 페이지 서비스 종료로 Yahoo Finance v7 배치 조회로 전환.

```
GET https://query1.finance.yahoo.com/v7/finance/quote
  ?symbols=005930.KS,000660.KS,...,247540.KQ,...
  &fields=shortName,regularMarketPrice,regularMarketChange,regularMarketChangePercent,regularMarketVolume
```

- KOSPI 주요 90종 + KOSDAQ 주요 26종을 한 번에 조회
- `regularMarketChangePercent` 기준으로 정렬 → 급등(desc) / 급락(asc)
- `regularMarketVolume` 기준 정렬 → 거래량(desc)
- 전체 상장 종목 대상이 아닌 주요 종목 대상이므로 소형주 급등 종목은 누락될 수 있음

---

### 3. 종목 검색 — KRX 데이터 포털

전체 KOSPI/KOSDAQ 검색 지원을 위해 앱 시작 시 KRX에서 전체 종목 로드 후 메모리 캐싱.

```java
// KrxStockListService.java — @PostConstruct로 앱 시작 시 1회 로드
POST https://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd
Body: bld=dbms/MDC/STAT/standard/MDCSTAT01901&locale=ko_KR&mktId=ALL

// 응답: { "OutBlock_1": [{ "ISU_SRT_CD": "005930", "ISU_ABBRV": "삼성전자", "SECUGRP_NM": "주권" }] }
// 필터: SECUGRP_NM == "주권" → 주식만 (ETF·수익증권 제외)
// 로드 실패 시: 주요 90개 FALLBACK 목록으로 자동 대체
```

- `POST /stocks` 요청은 외부 호출 없이 메모리 내 필터링 → 빠르고 안정적
- `watch_stock`·`my_stock`·`pending_order` 저장 시 `stock_name` 포함

---

### 4. lightweight-charts v5 API 변경 대응

v4 → v5에서 시리즈 추가 API가 완전히 바뀌었다.

```jsx
// v4 방식 (사용 불가)
chart.addCandlestickSeries({ ... })
chart.addHistogramSeries({ ... })

// v5 방식
import { CandlestickSeries, HistogramSeries, ColorType } from 'lightweight-charts';
chart.addSeries(CandlestickSeries, { ... })
chart.addSeries(HistogramSeries, { priceScaleId: 'vol' })
// 주의: priceScaleId에 'volume' 예약어 충돌 → 'vol' 사용

// 배경색 설정
background: { type: ColorType.Solid, color: '#141927' }
```

---

### 5. 캔들 timeframe 클라이언트 캐싱

일봉/주봉/월봉 탭 전환 시 매번 API 호출을 방지하기 위해 컴포넌트 내 캐싱 구현.

```jsx
const [candleCache, setCandleCache] = useState({});

const fetchCandles = async (tf) => {
  if (candleCache[tf]) { setTimeframe(tf); return; }  // 캐시 히트
  const res = await fetch(`/stock-detail/${code}?timeframe=${tf}`);
  setCandleCache(prev => ({ ...prev, [tf]: data.candles }));
};
```

---

### 6. 지정가 주문 시스템

**지원 주문 유형**

| 유형 | orderType | conditionType | 체결 조건 |
| --- | --- | --- | --- |
| 지정가 매수 | BUY | PRICE_BELOW | 현재가 ≤ 목표가 |
| 익절 매도 | SELL | PRICE_ABOVE | 현재가 ≥ 목표가 |
| 손절 매도 | SELL | PRICE_BELOW | 현재가 ≤ 목표가 |

**자동 체결 스케줄러**

- `PendingOrderScheduler`: 30초마다 전체 PENDING 주문 확인 → 조건 충족 시 자동 체결
- `JOIN FETCH p.customer`로 customer 즉시 로딩 (LazyInitializationException 방지)
- `checkAndExecuteAll()` 비트랜잭션 — 각 매수/매도는 독립 트랜잭션으로 처리
- 체결 시 TradeHistory 자동 기록

---

### 7. 선물 거래 시스템

**포지션 오픈 흐름**

```
POST /futures/open
  → account.deposit -= marginAmount  (증거금 예수금 차감)
  → FuturesPosition 생성 (status=OPEN, entryPrice=현재가)
```

**평가손익 계산**

```java
double priceChangePct = (currentPrice - entryPrice) / entryPrice;
int direction = position.getDirection() == LONG ? 1 : -1;
long unrealizedPnl = Math.round(marginAmount * leverage * priceChangePct * direction);
```

**포지션 청산 (정상)**

```
POST /futures/close
  → returnAmount = Math.max(0, marginAmount + realizedPnl)
  → account.deposit += returnAmount
  → status = CLOSED
```

**강제청산 (로스컷)**

```
FuturesLiquidationScheduler (60초 주기)
  → pnl <= -marginAmount 시 status = LIQUIDATED
  → 예수금 반환 없음 (증거금 전액 손실)
```

---

### 8. 카카오 로그인 — React Strict Mode 이중 실행 방지

React Strict Mode에서 `useEffect`가 개발 환경에 두 번 실행되어 OAuth authorization code가 소진되는 문제.

```jsx
// KakaoLogin.jsx
const didRun = useRef(false);
useEffect(() => {
    if (!code || didRun.current) return;
    didRun.current = true;
    doLogin();   // authorization code를 정확히 1회만 사용
}, [code]);
```

백엔드에서도 HTTP 200 이외 응답 시 RuntimeException을 throw하도록 수정 (기존: 빈 문자열 반환으로 카카오 사용자 조회 실패 시 성공/실패 토스트 동시 표시).

---

## 🗂️ 외부 데이터 소스

### Yahoo Finance v8 chart API (주 데이터 소스)

| 데이터 | 심볼 | interval / range |
| --- | --- | --- |
| 한국 주식 현재가·등락 | {코드}.KS 또는 .KQ | 1d / 1d |
| 한국 주식 캔들 (일봉) | {코드}.KS 또는 .KQ | 1d / 6mo |
| 한국 주식 캔들 (주봉) | {코드}.KS 또는 .KQ | 1wk / 2y |
| 한국 주식 캔들 (월봉) | {코드}.KS 또는 .KQ | 1mo / 5y |
| KOSPI 지수 | ^KS11 | 1d / 1d |
| KOSDAQ 지수 | ^KQ11 | 1d / 1d |
| 나스닥100 선물 | NQ=F | 1d / 1d |
| S&P500 선물 | ES=F | 1d / 1d |
| 금 선물 | GC=F | 1d / 1d |
| WTI원유 선물 | CL=F | 1d / 1d |
| 코스피200 선물 | ^KS200 (실패 시 KRX 폴백) | 1d / 1d |

**공통 요청 헤더**

```
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36
Accept: application/json
Accept-Language: en-US,en;q=0.9
Timeout: 7000ms
```

### Yahoo Finance v7 quote API (시세 랭킹 배치)

```
GET https://query1.finance.yahoo.com/v7/finance/quote?symbols={csv}
```

- KOSPI 90종 + KOSDAQ 26종을 한 요청으로 조회
- 응답: `quoteResponse.result[].{symbol, shortName, regularMarketPrice, regularMarketChange, regularMarketChangePercent, regularMarketVolume}`

### Yahoo Finance v10 quoteSummary (PER / PBR)

```
GET https://query1.finance.yahoo.com/v10/finance/quoteSummary/{symbol}?modules=summaryDetail
```

- 응답: `summaryDetail.trailingPE.fmt`, `summaryDetail.priceToBook.fmt`
- 조회 실패 시 PER/PBR 빈 문자열 처리 (선택적 데이터)

### KRX 데이터 포털

| 데이터 | BLD | 응답 키 | 주요 필드 |
| --- | --- | --- | --- |
| 전체 종목 목록 | MDCSTAT01901 | OutBlock_1 | ISU_SRT_CD, ISU_ABBRV, SECUGRP_NM |
| 주요 지수 (코스피200 폴백) | MDCSTAT00101 | output | IDX_NM, CLSPRC_IDX, PRV_DD_CMPR, UPDN_RATE |

```
POST https://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd
Content-Type: application/x-www-form-urlencoded; charset=UTF-8
Referer: https://data.krx.co.kr
```

---

## ⚠️ 알려진 제약사항

| 항목 | 내용 |
| --- | --- |
| 시세 랭킹 범위 | Yahoo v7 배치 대상 종목(KOSPI 90 + KOSDAQ 26)으로 제한 — 전체 상장 종목 대비 누락 가능 |
| Yahoo Finance 가용성 | 비공개 API로 언제든 응답 구조 변경 가능, 요청 과다 시 일시 차단(429) 가능 |
| 코스피200 선물 | ^KS200이 Yahoo에 없는 경우 KRX 포털 폴백 — 장 마감 후 최신 데이터 지연 가능 |
| PER / PBR | Yahoo v10 quoteSummary 응답 없을 시 공백 표시 |
| 선물 환율 미적용 | USD 선물의 KRW 손익은 단순 환산 없이 증거금(KRW) × 레버리지 × 가격변화율로 계산 |
| 실시간성 | Yahoo Finance는 거래소별 지연(KRX 15분, CME 10분) 데이터 제공 |
