# SeRVe Server

**Secure Repository for Vector Embeddings - MSA Backend**

Zero-Trust 기반 엣지 컴퓨팅 플랫폼의 백엔드 서버입니다. 로봇(Physical AI)이 수집한 데이터를 End-to-End 암호화하여 저장하고, 팀 단위로 안전하게 공유합니다.

## 기술 스택

- Java 17, Spring Boot 3.4.0, Spring Cloud 2024.0.0
- Spring Security + JWT (HS256)
- Spring Cloud OpenFeign (서비스 간 통신)
- Spring Data JPA + MariaDB
- Google Tink (AES-256-GCM / ECIES 암호화)
- Nginx (API Gateway)
- Docker Compose

## 아키텍처

```
클라이언트 (Streamlit / SDK)
        │
        ▼
   ┌─────────┐
   │  Nginx  │  :8080 (API Gateway)
   └────┬────┘
        │ URL 기반 라우팅
   ┌────┼──────────────┐
   ▼    ▼              ▼
 Auth  Team          Core
 :8081 :8082         :8083
   │    │              │
   ▼    ▼              ▼
 ┌──────────────────────┐
 │   MariaDB (RDS)      │
 │ ┌────┐┌────┐┌─────┐  │
 │ │auth││team││core │  │
 │ │_db ││_db ││_db  │  │
 │ └────┘└────┘└─────┘  │
 └──────────────────────┘
```

## 모듈 구조

```
SeRViS_server/
├── SeRVe-Common/    # 공통 모듈 (JWT, 암호화, 예외처리, 공유 DTO)
├── SeRVe-Auth/      # 인증 서비스 (:8081)
├── SeRVe-Team/      # 팀/멤버 관리 서비스 (:8082)
├── SeRVe-Core/      # 태스크/데모/동기화 서비스 (:8083)
├── gateway/         # Nginx API Gateway 설정
└── docker-compose.yml
```

### SeRVe-Common (공통 모듈)
모든 서비스에서 공유하는 유틸리티 및 설정
- `JwtTokenProvider` - JWT 발급/검증
- `JwtAuthenticationFilter` - JWT 인증 필터
- `CryptoManager` - Google Tink 암호화 유틸리티
- `GlobalExceptionHandler` - 공통 예외 처리
- `RateLimitService` - API 호출 제한
- 공유 Feign DTO (`common.dto.feign`)

### SeRVe-Auth (:8081)
사용자 인증 및 계정 관리
- 회원가입/로그인/로그아웃
- JWT 발급 (HS256)
- 비밀번호 재설정
- 공개키 조회 (멤버 초대용)
- DB: `serve_auth_db` (테이블: `users`)

### SeRVe-Team (:8082)
팀(저장소) 및 멤버 관리
- 저장소 CRUD
- 멤버 초대/강퇴/권한 변경
- 엣지노드(로봇) 등록/관리
- DB: `serve_team_db` (테이블: `teams`, `repository_members`, `edge_nodes`)

### SeRVe-Core (:8083)
태스크 저장, 벡터 데모, 데이터 동기화
- 태스크 업로드/다운로드 (E2E 암호화)
- 벡터 데모 관리
- 데이터 동기화 (Sync)
- 보안 키 교환 (Handshake)
- DB: `serve_core_db` (테이블: `tasks`, `encrypted_data`, `vector_demos`)

## 빌드 및 실행

### 사전 요구사항
- Java 17+
- Docker & Docker Compose

### 빌드

```bash
# 전체 빌드 (테스트 포함)
./gradlew build

# 특정 모듈 빌드
./gradlew :SeRVe-Auth:build
./gradlew :SeRVe-Team:build
./gradlew :SeRVe-Core:build
```

### 실행

```bash
# 1. DB + API Gateway 실행
docker-compose up -d mariadb gateway

# 2. 각 서비스 실행 (별도 터미널)
./gradlew :SeRVe-Auth:bootRun    # http://localhost:8081
./gradlew :SeRVe-Team:bootRun    # http://localhost:8082
./gradlew :SeRVe-Core:bootRun    # http://localhost:8083
```

클라이언트는 API Gateway(`http://localhost:8080`)를 통해 접속합니다.

### 테스트

```bash
# 전체 테스트
./gradlew test

# 모듈별 테스트
./gradlew :SeRVe-Auth:test
./gradlew :SeRVe-Team:test
./gradlew :SeRVe-Core:test
```

## API Gateway (Nginx)

Nginx가 `localhost:8080`에서 URL 패턴별로 각 서비스에 라우팅합니다.

| 경로 패턴 | 서비스 | 설명 |
|-----------|--------|------|
| `/auth/**` | Auth (:8081) | 인증 관련 |
| `/api/repositories` | Team (:8082) | 저장소 CRUD |
| `/api/teams/{id}/members/**` | Team (:8082) | 멤버 관리 |
| `/edge-nodes/**` | Team (:8082) | 엣지노드 관리 |
| `/api/tasks` | Core (:8083) | 태스크 업로드 |
| `/api/tasks/{id}` | Core (:8083) | 태스크 다운로드 |
| `/api/teams/{id}/tasks` | Core (:8083) | 팀별 태스크 관리 |
| `/api/teams/{id}/demos` | Core (:8083) | 벡터 데모 |
| `/api/security/**` | Core (:8083) | 키 교환 |
| `/api/sync/**` | Core (:8083) | 데이터 동기화 |

## 서비스 간 통신

Spring Cloud OpenFeign을 사용한 REST 통신. 내부 API는 `/internal/**` 경로로 노출되며, SecurityConfig에서 `permitAll` 처리됩니다.

```
Auth ←── Team (사용자 정보 조회)
Auth ←── Core (사용자 존재 확인)
Team ←── Core (팀/멤버 존재 확인, 권한 조회)
```

## 보안

- **JWT (HS256)**: Auth 서비스에서만 발급, 다른 서비스는 공유 Secret으로 검증만 수행
- **Zero-Trust**: 서버는 평문 데이터나 사용자 개인키를 절대 보지 못함
- **Envelope Encryption**: 팀 키(DEK)를 사용자 공개키(KEK)로 래핑하여 저장
- **Google Tink**: AES-256-GCM (데이터 암호화), ECIES (키 래핑)

## 클라이언트

`client-robot/` 디렉토리에 Python SDK 및 Streamlit 대시보드가 있습니다. 자세한 내용은 `README_Client.md`를 참고하세요.

```bash
cd client-robot
pip install -r requirements.txt
streamlit run app.py    # http://localhost:8501
```

## 민감정보 관리

- `application.yml`에 DB 비밀번호, JWT Secret 등 민감정보를 직접 커밋하지 마세요
- 로컬 설정은 `.gitignore`에 추가되어 있습니다
- CI/배포 시에는 환경변수 또는 GitHub Secrets를 사용하세요
