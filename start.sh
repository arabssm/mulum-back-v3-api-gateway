#!/bin/bash

MODE=${1:-dev}

echo "API Gateway 시작..."
echo "모드: $MODE"

echo "기존 프로세스 확인 중..."
PID=$(lsof -ti:8080 2>/dev/null)
if [ ! -z "$PID" ]; then
    kill -9 $PID
    echo "기존 프로세스 종료 완료 (PID: $PID)"
    sleep 2
else
    echo "실행 중인 프로세스 없음"
fi

echo "환경변수 로드 중..."
if [ "$MODE" = "prod" ]; then
    if [ -f .env.production ]; then
        export $(grep -v '^#' .env.production | xargs)
        echo ".env.production 로드 완료"
    else
        echo ".env.production 파일을 찾을 수 없습니다!"
        exit 1
    fi
else
    if [ -f .env ]; then
        export $(grep -v '^#' .env | xargs)
        echo ".env 로드 완료"
    else
        echo ".env 파일을 찾을 수 없습니다!"
        exit 1
    fi
fi

echo "🔨 프로젝트 빌드 중..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo "빌드 실패!"
    exit 1
fi

echo "빌드 완료"
mkdir -p logs

JAR_FILE=$(find build/libs -name "*.jar" | grep -v "plain.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "JAR 파일을 찾을 수 없습니다!"
    exit 1
fi

echo "JAR 파일: $JAR_FILE"

echo "애플리케이션 시작 중..."

if [ "$MODE" = "prod" ]; then
    nohup java -jar $JAR_FILE > logs/application.log 2>&1 &
    APP_PID=$!
    echo "애플리케이션 시작 완료 (PID: $APP_PID)"
    echo "로그 확인: tail -f logs/application.log"
    echo "종료 방법: kill -9 $APP_PID 또는 kill -9 \$(lsof -ti:8080)"
else
    ./gradlew bootRun
fi

