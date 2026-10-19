# StockNest

AI-powered stock trading simulator — built with vibe coding.

## Tech Stack

| Layer | Tech |
|-------|------|
| Backend | Java 17, Spring Boot 3.2, Spring Data JPA, WebSocket STOMP |
| Frontend | React 18, TradingView lightweight-charts, CSS Modules |
| AI | Google Gemini LLM (SSE streaming), RAG (ChromaDB + Gemini Embedding) |
| Database | MySQL 9 |
| Automation | n8n (Docker), Scheduled tasks |
| Data Sources | Yahoo Finance v8, Naver Finance API, KRX |

## Features

- Real-time stock price streaming (WebSocket STOMP, 30s interval)
- Candlestick chart with MA5/MA20/MA60 overlays (day/week/month)
- Market & limit order execution
- Futures trading — LONG/SHORT, leverage (2x~20x), auto-liquidation
- ETF browser (25 Korean ETFs, 6 categories)
- AI investment assistant (Gemini LLM, SSE streaming, model fallback chain)
- RAG pipeline (ChromaDB vector search, 30 financial knowledge docs)
- Notification system with Kakao push messaging
- Trade history & portfolio analytics
- n8n workflow automation (daily AI insights, RAG health check)
- Kakao OAuth login
- Responsive design (mobile / tablet / desktop)

## Getting Started

```bash
# Backend
cd back
./mvnw spring-boot:run

# Frontend
cd front
npm install && npm start

# Docker services (ChromaDB + n8n)
docker-compose up -d
```

## Architecture

```
Browser (React 18)
    |
    +-- REST API / WebSocket / SSE
    |
Spring Boot 3.2
    +-- MySQL (JPA)
    +-- Yahoo Finance / Naver API / KRX
    +-- Gemini API (LLM + Embedding)
    +-- ChromaDB (Vector DB)
    +-- n8n (Webhook automation)
```

---

Built with AI (vibe coding) by [@dohee789](https://github.com/dohee789)
