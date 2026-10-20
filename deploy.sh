#!/bin/bash
set -e

echo "============================================"
echo "  삐약스탁 배포 스크립트"
echo "============================================"

# ── 1. 환경변수 확인 ──
if [ ! -f .env ]; then
    echo ""
    echo "[ERROR] .env 파일이 없습니다!"
    echo "  cp .env.example .env  로 복사한 뒤 값을 설정해주세요."
    echo ""
    exit 1
fi

source .env

if [ -z "$GEMINI_API_KEY" ] || [ "$GEMINI_API_KEY" = "your_gemini_api_key_here" ]; then
    echo "[ERROR] .env에 GEMINI_API_KEY를 설정해주세요."
    exit 1
fi

if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" = "your_secure_password_here" ]; then
    echo "[ERROR] .env에 DB_PASSWORD를 설정해주세요."
    exit 1
fi

echo "[OK] PostgreSQL 사용"

echo "[OK] 환경변수 확인 완료"

# ── 2. Docker Compose 빌드 & 실행 ──
echo ""
echo ">>> Docker Compose 빌드 시작..."
docker compose -f docker-compose.prod.yml --env-file .env up -d --build

echo ""
echo ">>> 서비스 상태 확인..."
sleep 5
docker compose -f docker-compose.prod.yml ps

echo ""
echo "============================================"
echo "  배포 완료!"
echo ""
echo "  Backend API:  http://$(curl -s ifconfig.me):${SERVER_PORT:-8090}"
echo "  Nginx:        http://$(curl -s ifconfig.me)"
echo "  ChromaDB:     http://localhost:8100"
echo "  n8n:          http://localhost:5678"
echo ""
echo "  다음 단계:"
echo "  1. Vercel에서 프론트엔드 배포"
echo "     REACT_APP_API_URL=http://서버IP"
echo "  2. (선택) 도메인 연결 + HTTPS 설정"
echo "============================================"
