# GitHub Actions CD 설정 가이드

## 📋 필요한 GitHub Secrets

GitHub 리포지토리 Settings > Secrets and variables > Actions에서 다음 secrets를 추가하세요:

### 🔐 EC2 접속 정보
- `EC2_HOST`: EC2 퍼블릭 IP 또는 도메인 (예: 1.2.3.4)
- `EC2_USERNAME`: EC2 사용자명 (예: ec2-user, ubuntu)
- `EC2_SSH_KEY`: EC2 접속용 SSH Private Key (PEM 파일 내용 전체)

### 🔧 애플리케이션 환경변수
- `JWT_SECRET_KEY`: JWT 암호화 키
- `SERVER_PORT`: 서버 포트 (예: 8080)
- `USER_SERVICE_URL`: User Service URL
- `ITEM_SERVICE_URL`: Item Service URL
- `AUTH_SERVICE_URL`: Auth Service URL
- `CALENDAR_SERVICE_URL`: Calendar Service URL
- `TEAM_SERVICE_URL`: Team Service URL
- `LOG_LEVEL`: 로그 레벨 (예: INFO)

## 🚀 배포 방법

### 자동 배포
- `main` 브랜치에 push하면 자동으로 배포됩니다
- `develop` 브랜치에 push하면 자동으로 배포됩니다

### 수동 배포
1. GitHub 리포지토리로 이동
2. Actions 탭 클릭
3. "CD - Deploy to EC2" 워크플로우 선택
4. "Run workflow" 버튼 클릭

## 📝 SSH Key 설정 방법

### 1. EC2 SSH Private Key 가져오기
```bash
cat your-key.pem
```

### 2. GitHub Secrets에 등록
- 전체 내용을 복사 (-----BEGIN RSA PRIVATE KEY----- 부터 -----END RSA PRIVATE KEY----- 까지)
- `EC2_SSH_KEY`에 붙여넣기

## 🔍 배포 프로세스

1. ✅ 코드 체크아웃
2. ✅ JDK 21 설정
3. ✅ Gradle 캐시 로드
4. ✅ 프로젝트 빌드
5. ✅ JAR 파일 EC2로 전송
6. ✅ 기존 프로세스 종료
7. ✅ 새 애플리케이션 실행
8. ✅ 헬스체크

## 🛡️ EC2 보안 그룹 설정

다음 포트가 열려있어야 합니다:
- 22 (SSH): GitHub Actions에서 접속
- 8080 (HTTP): 애플리케이션 포트

## ⚠️ 주의사항

1. EC2에 Java 21이 설치되어 있어야 합니다
2. EC2 보안 그룹에서 SSH(22) 포트가 열려있어야 합니다
3. SSH Key는 반드시 안전하게 관리하세요
4. .env.production 파일은 Git에 커밋하지 마세요

