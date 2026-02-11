SeRVe MSA 멀티모듈 리팩토링 계획

Context

현재 SeRVe는 단일 Spring Boot 모놀리식 앱(horizon.SeRVe 패키지)으로, 모든 엔티티/서비스/컨트롤러가 하나의 DB(serve_db)를 공유합니다.
CLAUDE.md에 정의된 대로 Gradle 멀티모듈 MSA(SeRVe-Common, SeRVe-Auth, SeRVe-Team, SeRVe-Core)로 분리하며, 서비스 간 통신은 Spring Cloud OpenFeign을 사용합니다.

 ---
Phase 0: 루트 빌드 설정 및 디렉토리 스캐폴딩

0.1 settings.gradle 수정

rootProject.name = 'SeRVe'
include 'SeRVe-Common', 'SeRVe-Auth', 'SeRVe-Team', 'SeRVe-Core'

0.2 루트 build.gradle 재작성

- allprojects: group=horizon.SeRVe, repositories
- subprojects: Java 17 toolchain, Lombok(compile/test), spring-boot-starter-test, JUnit Platform
- Spring Boot/dependency-management 플러그인은 apply false로 선언

0.3 4개 모듈 디렉토리 생성

SeRVe-Common/src/main/java/com/serve/common/
SeRVe-Auth/src/main/java/com/serve/auth/ + src/main/resources/
SeRVe-Team/src/main/java/com/serve/team/ + src/main/resources/
SeRVe-Core/src/main/java/com/serve/core/ + src/main/resources/

검증: ./gradlew projects → 4개 서브프로젝트 확인

 ---
Phase 1: SeRVe-Common 모듈

라이브러리 모듈(bootJar 없음). 모든 서비스 모듈이 의존.

1.1 SeRVe-Common/build.gradle

- java-library 플러그인 + Spring Boot BOM(dependencyManagement)
- api 의존성: spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-validation, JJWT, Google Tink, protobuf, guava

1.2 이동할 파일 (패키지만 변경)
┌───────────────────────────────────────────┬──────────────────────────────────┐
│                   원본                    │           대상 패키지            │
├───────────────────────────────────────────┼──────────────────────────────────┤
│ security/crypto/CryptoManager.java        │ horizon.SeRVe.common.security.crypto │
├───────────────────────────────────────────┼──────────────────────────────────┤
│ security/crypto/KeyExchangeService.java   │ horizon.SeRVe.common.security.crypto │
├───────────────────────────────────────────┼──────────────────────────────────┤
│ exception/GlobalExceptionHandler.java     │ horizon.SeRVe.common.exception       │
├───────────────────────────────────────────┼──────────────────────────────────┤
│ exception/RateLimitExceededException.java │ horizon.SeRVe.common.exception       │
├───────────────────────────────────────────┼──────────────────────────────────┤
│ service/RateLimitService.java             │ horizon.SeRVe.common.service         │
└───────────────────────────────────────────┴──────────────────────────────────┘
1.3 JwtTokenProvider 리팩토링 (핵심 변경)

원본: config/JwtTokenProvider.java → horizon.SeRVe.common.security.jwt.JwtTokenProvider

변경 사항: UserDetailsService 의존성 제거. Team/Core 서비스에는 users 테이블이 없으므로 getAuthentication()이 DB 조회 없이 JWT claims(userId, email)만으로 Authentication 객체를
생성하도록 변경.

public Authentication getAuthentication(String token) {
String email = getUserEmail(token);
String userId = getUserId(token);  // NEW 메서드
List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
UsernamePasswordAuthenticationToken auth =
new UsernamePasswordAuthenticationToken(userId, null, authorities);
auth.setDetails(Map.of("email", email, "userId", userId));
return auth;
}

1.4 JwtAuthenticationFilter 이동

config/JwtAuthenticationFilter.java → horizon.SeRVe.common.security.jwt (로직 변경 없음)

1.5 서비스 간 Feign 통신용 공통 DTO 생성

- horizon.SeRVe.common.dto.feign.UserInfoResponse (userId, email, publicKey)
- horizon.SeRVe.common.dto.feign.EdgeNodeAuthResponse (nodeId, serialNumber, hashedToken, publicKey, encryptedTeamKey, teamId)
- horizon.SeRVe.common.dto.feign.MemberRoleResponse (userId, teamId, role, encryptedTeamKey)

검증: ./gradlew :SeRVe-Common:build

 ---
Phase 2: SeRVe-Auth 모듈

독립 Spring Boot 앱. 포트: 8081. DB: serve_auth_db (users 테이블)

2.1 SeRVe-Auth/build.gradle

- Spring Boot + dependency-management + Spring Cloud BOM(2024.0.0)
- 의존성: project(':SeRVe-Common'), spring-boot-starter-data-jpa, mariadb-java-client, H2, spring-cloud-starter-openfeign

2.2 application.yml

- port: 8081, DB: serve_auth_db, jwt.secret/expiration, service.team.url: ${TEAM_SERVICE_URL:http://localhost:8082}

2.3 이동할 파일
┌───────────────────────────────────────┬───────────────────────────┬───────────────────────────────────────────────────────┐
│                 원본                  │        대상 패키지        │                       변경 사항                       │
├───────────────────────────────────────┼───────────────────────────┼───────────────────────────────────────────────────────┤
│ entity/User.java                      │ horizon.SeRVe.auth.entity     │ 패키지만                                              │
├───────────────────────────────────────┼───────────────────────────┼───────────────────────────────────────────────────────┤
│ repository/UserRepository.java        │ horizon.SeRVe.auth.repository │ 패키지만                                              │
├───────────────────────────────────────┼───────────────────────────┼───────────────────────────────────────────────────────┤
│ service/CustomUserDetailsService.java │ horizon.SeRVe.auth.service    │ 패키지만                                              │
├───────────────────────────────────────┼───────────────────────────┼───────────────────────────────────────────────────────┤
│ service/AuthService.java              │ horizon.SeRVe.auth.service    │ EdgeNodeRepository → TeamServiceClient Feign으로 교체 │
├───────────────────────────────────────┼───────────────────────────┼───────────────────────────────────────────────────────┤
│ controller/AuthController.java        │ horizon.SeRVe.auth.controller │ 패키지만                                              │
├───────────────────────────────────────┼───────────────────────────┼───────────────────────────────────────────────────────┤
│ dto/auth/* 전체                       │ horizon.SeRVe.auth.dto        │ 패키지만                                              │
└───────────────────────────────────────┴───────────────────────────┴───────────────────────────────────────────────────────┘
2.4 AuthService.robotLogin() 변경

현재: edgeNodeRepository.findBySerialNumber() 직접 호출
변경: TeamServiceClient.getEdgeNodeBySerial() Feign 호출 → EdgeNodeAuthResponse로 hashedToken 비교 후 JWT 발급

2.5 Auth 전용 SecurityConfig

- /auth/**, /internal/** 허용
- Auth만의 AuthJwtAuthenticationFilter 생성: UserDetailsService를 사용해 full User 엔티티를 SecurityContext에 로드 (Auth 컨트롤러의 @AuthenticationPrincipal User user 유지)

2.6 Feign Client 생성

@FeignClient(name = "serve-team", url = "${service.team.url}")
public interface TeamServiceClient {
@GetMapping("/internal/edge-nodes/by-serial/{serialNumber}")
EdgeNodeAuthResponse getEdgeNodeBySerial(@PathVariable String serialNumber);
}

2.7 Internal API 컨트롤러 생성 (다른 서비스가 Auth를 호출)

InternalUserController (/internal/users/**)
- GET /internal/users/{userId} → UserInfoResponse
- GET /internal/users/by-email/{email} → UserInfoResponse
- GET /internal/users/{userId}/exists → Boolean
- GET /internal/users/{userId}/public-key → String

2.8 SeRVeAuthApplication.java 생성

@SpringBootApplication(scanBasePackages = {"horizon.SeRVe.auth", "horizon.SeRVe.common"}) + @EnableFeignClients

검증: ./gradlew :SeRVe-Auth:build

 ---
Phase 3: SeRVe-Team 모듈

독립 Spring Boot 앱. 포트: 8082. DB: serve_team_db (teams, repository_members, edge_nodes)

3.1 SeRVe-Team/build.gradle

- Spring Boot + Spring Cloud BOM
- 의존성: project(':SeRVe-Common'), spring-boot-starter-data-jpa, mariadb-java-client, H2, spring-cloud-starter-openfeign

3.2 application.yml

- port: 8082, DB: serve_team_db, jwt.secret, service.auth.url: ${AUTH_SERVICE_URL:http://localhost:8081}

3.3 엔티티 변경 (핵심)

RepositoryMember.java — User JPA 참조 제거:
// 삭제: @MapsId("userId") @ManyToOne User user;
// 추가:
@Column(name = "user_id", insertable = false, updatable = false)
private String userId;

EdgeNode.java — implements UserDetails 제거 (더 이상 Spring Security principal이 아님)

Team, RepositoryMemberId, Role, RepoType — 패키지 변경만

3.4 Repository 변경

MemberRepository 쿼리 시그니처 변경:
- findAllByUser(User) → findAllByUserId(String)
- findByTeamAndUser(Team, User) → findByTeamAndUserId(Team, String)
- existsByTeamAndUser(Team, User) → existsByTeamAndUserId(Team, String)

TeamRepository, EdgeNodeRepository — 패키지 변경만

3.5 서비스 변경

RepoService, MemberService — UserRepository 의존성을 AuthServiceClient Feign으로 교체:
- userRepository.findById(userId) → userId String 직접 사용 (멤버십 관리에는 User 엔티티 불필요)
- userRepository.findByEmail(email) → authServiceClient.getUserByEmail(email) (초대 시 userId 조회)
- memberRepository.findByTeamAndUser(team, user) → memberRepository.findByTeamAndUserId(team, userId)
- Owner email 표시: authServiceClient.getUserInfo(ownerId).getEmail()

EdgeNodeService — 패키지/임포트 변경만 (Team과 EdgeNode 모두 같은 DB)

3.6 컨트롤러 변경

모든 컨트롤러: @AuthenticationPrincipal User user → Authentication authentication + (String) authentication.getPrincipal()로 userId 추출

3.7 DTO 변경

- MemberResponse.from(RepositoryMember) → MemberResponse.from(RepositoryMember, String email) (email은 Feign으로 조회)
- RepoResponse.of(Team, User) → RepoResponse.of(Team, String ownerEmail)

3.8 Feign Client 생성

@FeignClient(name = "serve-auth", url = "${service.auth.url}")
public interface AuthServiceClient {
@GetMapping("/internal/users/{userId}") UserInfoResponse getUserInfo(@PathVariable String userId);
@GetMapping("/internal/users/by-email/{email}") UserInfoResponse getUserByEmail(@PathVariable String email);
@GetMapping("/internal/users/{userId}/exists") Boolean userExists(@PathVariable String userId);
}

3.9 Internal API 컨트롤러 생성 (Auth와 Core가 Team을 호출)

InternalTeamController (/internal/**)
- GET /internal/teams/{teamId}/exists → Boolean
- GET /internal/teams/{teamId}/members/{userId}/role → MemberRoleResponse
- GET /internal/teams/{teamId}/members/{userId}/exists → Boolean
- GET /internal/edge-nodes/by-serial/{serialNumber} → EdgeNodeAuthResponse
- GET /internal/edge-nodes/{nodeId}/team-id → String

3.10 Team SecurityConfig

- /internal/**, /edge-nodes/register 허용, 나머지 인증 필요
- Common의 JwtAuthenticationFilter 사용 (principal = userId String)

검증: ./gradlew :SeRVe-Team:build

 ---
Phase 4: SeRVe-Core 모듈

독립 Spring Boot 앱. 포트: 8083. DB: serve_core_db (tasks, encrypted_data, vector_demos)

4.1 SeRVe-Core/build.gradle

- Spring Boot + Spring Cloud BOM
- 의존성: project(':SeRVe-Common'), spring-boot-starter-data-jpa, mariadb-java-client, H2, spring-cloud-starter-openfeign

4.2 application.yml

- port: 8083, DB: serve_core_db, jwt.secret, service.team.url, service.auth.url

4.3 엔티티 변경 (핵심)

Task.java — Team/User JPA 참조를 String ID로 교체:
// 삭제: @ManyToOne Team team; @ManyToOne User uploader;
// 추가:
@Column(name = "team_id")
private String teamId;
@Column(name = "uploader_id")
private String uploaderId;

EncryptedData, VectorDemo — 패키지 변경만 (이미 String ID 사용)

4.4 Repository 변경

TaskRepository 쿼리 변경:
- findAllByTeam(Team) → findAllByTeamId(String)
- findByTeamAndOriginalFileName(Team, String) → findByTeamIdAndOriginalFileName(String, String)

EncryptedDataRepository, VectorDemoRepository — 패키지 변경만

4.5 서비스 변경 (가장 큰 변경)

TaskService — TeamRepository, UserRepository, MemberRepository, EdgeNodeRepository 모두 제거. Feign으로 교체:
- 팀 존재 확인: teamServiceClient.teamExists(teamId)
- 멤버십/권한 확인: teamServiceClient.getMemberRole(teamId, userId)
- 멤버십 존재 확인: teamServiceClient.memberExists(teamId, userId)
- EdgeNode 팀 확인: teamServiceClient.getEdgeNodeTeamId(nodeId)
- Task 생성 시: .teamId(teamId), .uploaderId(userId) (String)

DemoService — 동일 패턴으로 Feign 교체

SyncService — teamServiceClient.teamExists(teamId) + taskRepository.findAllByTeamId(teamId)

4.6 컨트롤러 변경

- @AuthenticationPrincipal User user → Authentication + userId 추출
- SecurityController (crypto handshake) → Core 모듈로 이동 (/api/security/** 공개)

4.7 DTO 변경

- TaskResponse.from(): task.getUploader().getUserId() → task.getUploaderId()
- ChangedTaskResponse.from(): 동일 패턴

4.8 Feign Clients 생성

@FeignClient(name = "serve-team", url = "${service.team.url}")
public interface TeamServiceClient { /* teamExists, getMemberRole, memberExists, getEdgeNodeTeamId */ }

@FeignClient(name = "serve-auth", url = "${service.auth.url}")
public interface AuthServiceClient { /* getUserInfo, userExists */ }

4.9 Core SecurityConfig

- /api/security/**, /internal/** 허용, 나머지 인증 필요

검증: ./gradlew :SeRVe-Core:build → ./gradlew build (전체)

 ---
Phase 5: 인프라 및 정리

5.1 Docker Compose 업데이트

- docker/init-db.sql 생성: 3개 스키마(serve_auth_db, serve_team_db, serve_core_db) 생성 + 권한 부여
- docker-compose.yml: init-db.sql을 /docker-entrypoint-initdb.d/에 마운트, 기존 MARIADB_DATABASE 환경변수 제거

5.2 CI/CD 업데이트

- .github/workflows/gradle.yml: 기존과 동일 (./gradlew build가 모든 모듈을 빌드)

5.3 기존 코드 삭제

- src/main/java/horizon/ 디렉토리 삭제
- src/test/java/horizon/ 디렉토리 삭제
- 루트 application.properties 삭제

5.4 테스트 분배

- 단위 테스트(Mockito): 각 모듈로 분배 (패키지/임포트 업데이트)
- 통합 테스트: @SpringBootTest 사용하는 테스트는 해당 모듈 내에서 Feign mock으로 수정
- CryptoTest → SeRVe-Common
- AuthServiceTest, AuthControllerTest → SeRVe-Auth
- RepoServiceTest, MemberServiceTest → SeRVe-Team
- TaskServiceTest, DemoServiceTest → SeRVe-Core

 ---
실행 순서 요약

1. Phase 0 → ./gradlew projects 확인
2. Phase 1 (Common) → ./gradlew :SeRVe-Common:build 확인
3. Phase 2 (Auth) → ./gradlew :SeRVe-Auth:build 확인
4. Phase 3 (Team) → ./gradlew :SeRVe-Team:build 확인
5. Phase 4 (Core) → ./gradlew :SeRVe-Core:build 확인
6. 전체 빌드: ./gradlew build 확인
7. Phase 5 (인프라 + 정리)

각 Phase 완료 후 빌드 검증을 거치므로, 문제 발생 시 해당 Phase 내에서 즉시 수정 가능.

 ---
주요 리스크 및 대응
┌──────────────────────────────────────────────┬────────────────────────────────────────────────────────┬────────────────────────────────────────────────────────────────────┐
│                    리스크                      │                          영향                           │                                대응                                 
├──────────────────────────────────────────────┼────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────┤
│ JwtTokenProvider에서 UserDetailsService 제거   │ Auth의 @AuthenticationPrincipal User 패턴 깨질 수 있음      │ Auth 전용 AuthJwtAuthenticationFilter로 full UserDetails 로드 유지     │
├──────────────────────────────────────────────┼────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────┤
│ RepositoryMember에서 User JPA 참조 제거         │ MemberRepository 쿼리 14+ 호출 지점 변경                    │ findByTeamAndUserId(Team, String) 패턴으로 일괄 교체                    │
├──────────────────────────────────────────────┼────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────┤
│ Task에서 Team/User JPA 참조 제거                  │ TaskRepository 쿼리 6+ 호출 지점 변경                      │ findByTeamIdAndOriginalFileName(String, String) 패턴으로 교체          │
├──────────────────────────────────────────────┼────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────┤
│ Feign 호출 실패 (네트워크 오류)                    │ 서비스 간 통신 실패                                        │ 1차: fail-fast, 2차(추후): Resilience4j 서킷브레이커                     │
└──────────────────────────────────────────────┴────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────┘
