# n8n 워크플로우 설정 가이드

## 접속 정보
- URL: http://localhost:5678
- ID: admin
- PW: admin123

## 워크플로우 1: 일일 AI 마켓 인사이트 (매일 오전 9시)

### 노드 구성
1. **Schedule Trigger** → 매일 오전 9:00 (Asia/Seoul)
2. **HTTP Request** → POST http://host.docker.internal:8090/api/n8n/daily-insight
3. **IF** → `{{ $json.sent > 0 }}` 체크

### 설정 방법
1. n8n 대시보드에서 "New Workflow" 클릭
2. "Schedule Trigger" 노드 추가 → Cron Expression: `0 9 * * *`
3. "HTTP Request" 노드 추가:
   - Method: POST
   - URL: http://host.docker.internal:8090/api/n8n/daily-insight
   - Header: Content-Type: application/json
4. 워크플로우 활성화

## 워크플로우 2: RAG 헬스 체크 (매시간)

### 노드 구성
1. **Schedule Trigger** → 매시간
2. **HTTP Request** → GET http://host.docker.internal:8090/api/n8n/health
3. **IF** → `{{ $json.ragReady === false }}`
4. **HTTP Request** → POST http://host.docker.internal:8090/api/n8n/rag/refresh

### 설정 방법
1. "Schedule Trigger" 노드 → Cron: `0 * * * *`
2. "HTTP Request" 노드 → GET http://host.docker.internal:8090/api/n8n/health
3. "IF" 노드 → ragReady가 false이면 refresh 호출

## 워크플로우 3: 주간 포트폴리오 리포트 (매주 금요일 18시)

### 노드 구성
1. **Schedule Trigger** → 매주 금요일 18:00
2. **HTTP Request** → GET http://host.docker.internal:8090/api/n8n/health (서버 확인)
3. **HTTP Request** → POST http://host.docker.internal:8090/api/n8n/daily-insight

## 참고사항
- Docker 환경에서 호스트 Spring Boot 서버에 접근할 때 `host.docker.internal` 사용
- Mac/Windows에서는 자동 지원, Linux에서는 docker-compose에 extra_hosts 추가 필요
