FROM amazoncorretto:21-alpine

WORKDIR /app

# JAR 파일 복사
COPY build/libs/app.jar app.jar

# 로그 디렉토리 생성
RUN mkdir -p logs

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

