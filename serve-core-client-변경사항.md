# serve-core 서버 변경사항 및 클라이언트 대응 가이드

> **대상:** client-robot 담당자
> **작업 브랜치:** `dev` (추후 `main` 머지 예정)
> **핵심 요약:** 바이너리 저장 방식이 DB LONGBLOB → S3로 전환됨. 동기화 API 응답 스펙이 변경됨.

---

## 1. 변경 배경

기존에는 암호화된 바이너리(Task, Demo 데이터)를 DB의 LONGBLOB 컬럼에 직접 저장하고,
sync API 응답에 바이너리를 통째로 포함하여 내려줬습니다.

이 방식은 RDS 용량/성능 부담, 네트워크 부담이 크므로,
**"S3에 바이너리 저장 + DB에는 objectKey(S3 경로 포인터)만 저장"** 구조로 전환했습니다.

---

## 2. 서버 변경사항 요약

### 2-1. S3 스토리지 연동

- 모든 암호화 바이너리(Task, VectorDemo)를 S3 버킷(`servis-artifacts`)에 저장
- DB에는 S3 key(경로 문자열)만 저장

**S3 objectKey 형식:**

| 종류 | objectKey 형식 |
|------|----------------|
| Task | `{teamId}/{taskId}/task/{fileName}` |
| VectorDemo | `{teamId}/{taskId}/demo/demo_{demoIndex}.enc` |
| Artifact | `{teamId}/{scenarioId}/{demoId}/{filename}` |

### 2-2. 신규 DB 테이블 3개 추가

로봇 데이터 파이프라인을 위한 메타데이터 테이블이 추가됐습니다.
기존 `tasks`, `vector_demos`, `encrypted_data` 테이블은 그대로 유지됩니다.

| 테이블 | 용도 |
|--------|------|
| `scenarios` | 시나리오(prompt) 관리. promptHash로 중복 방지 |
| `demos` | 에피소드 메타데이터 (numSteps, stateDim 등). Scenario에 종속 |
| `artifacts` | S3 아티팩트 포인터. Demo에 종속. objectKey + 암호화 메타 저장 |

---

## 3. 클라이언트가 수정해야 할 부분

### 3-1. ⚠️ Task 다운로드 API 응답 변경 (Breaking Change)

**엔드포인트:** `GET /api/tasks/{id}/data`

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 응답 필드 | `content` (byte[], Base64) | `objectKey` (String, S3 key) |

**변경 전 응답:**
```json
{
  "id": 42,
  "content": "8J+Qju+4jy...",
  "version": 1
}
```

**변경 후 응답:**
```json
{
  "id": 42,
  "objectKey": "team-uuid/task-uuid/task/model.enc",
  "version": 1
}
```

**클라이언트 수정 방향:**
```
기존: response["content"] → Base64 디코딩 → 사용
변경: response["objectKey"] → S3에서 직접 다운로드 → 사용
```

---

### 3-2. ⚠️ Demo 동기화 API 응답 변경 (Breaking Change)

**엔드포인트:** `GET /api/sync/demos`

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 응답 필드 | `encryptedBlob` (byte[], Base64) | `objectKey` (String, S3 key) |

**변경 전 응답 (배열):**
```json
[
  {
    "taskId": "...",
    "demoId": "...",
    "demoIndex": 0,
    "encryptedBlob": "8J+Qju+4jy...",
    "version": 3,
    "isDeleted": false,
    "createdBy": "user@example.com"
  }
]
```

**변경 후 응답 (배열):**
```json
[
  {
    "taskId": "...",
    "demoId": "...",
    "demoIndex": 0,
    "objectKey": "team-uuid/task-uuid/demo/demo_0.enc",
    "version": 3,
    "isDeleted": false,
    "createdBy": "user@example.com"
  }
]
```

**클라이언트 수정 방향:**
```
기존: item["encryptedBlob"] → Base64 디코딩 → 로컬 저장
변경: item["objectKey"] → S3에서 직접 다운로드 → 로컬 저장
```

---

### 3-3. S3 다운로드 방식

서버가 바이너리를 직접 내려주지 않으므로, 클라이언트가 `objectKey`를 사용하여
**AWS S3에서 직접 다운로드**해야 합니다.

S3 접근 방식은 두 가지입니다:

**방식 A — AWS SDK 직접 사용 (boto3):**
```python
import boto3

s3 = boto3.client('s3', region_name='ap-northeast-2')
bucket = 'servis-artifacts'

# objectKey는 sync API 응답에서 수신
object_key = response['objectKey']
s3.download_file(bucket, object_key, '/local/path/to/save')
```

**방식 B — 서버에 presigned URL 요청 (추후 서버 API 추가 시):**
현재는 미구현. 필요 시 서버 측에 `GET /api/artifacts/{artifactId}/presigned-url` 추가 예정.

> **IAM 권한:** 로봇 디바이스가 S3에 직접 접근하려면 `servis-artifacts` 버킷에 대한
> `s3:GetObject` 권한이 있는 IAM 자격증명이 필요합니다. 인프라 담당자에게 요청하세요.

---

### 3-4. Task 동기화 API는 변경 없음

**엔드포인트:** `GET /api/sync/tasks`

`ChangedTaskResponse`는 원래 바이너리를 포함하지 않았으므로 응답 스펙 변경 없습니다.
기존 클라이언트 코드 그대로 사용 가능합니다.

---

## 4. 신규 API (선택적 사용)

서버에 Scenario/Demo/Artifact 파이프라인 API가 추가됐습니다.
로봇 에피소드 메타데이터를 서버에 등록하고 싶을 경우 사용할 수 있습니다.

### Scenario API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/scenarios` | Scenario 등록 (동일 prompt면 기존 반환) |
| GET | `/api/scenarios` | 전체 목록 조회 |
| GET | `/api/scenarios/{scenarioId}` | 단건 조회 |
| GET | `/api/scenarios/{scenarioId}/demos` | Scenario별 Demo 목록 |

**POST /api/scenarios 요청:**
```json
{
  "promptText": "Pick up the red cube and place it on the table."
}
```

### Artifact API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/demos/{demoId}/artifacts` | Artifact 등록 (S3 업로드 + 메타 저장) |
| GET | `/api/demos/{demoId}/artifacts` | Demo의 Artifact 목록 |
| GET | `/api/artifacts/{artifactId}` | 단건 조회 (objectKey 포함) |

**POST /api/demos/{demoId}/artifacts 요청:**
```json
{
  "teamId": "team-uuid",
  "kind": "processed",
  "encryptedData": "<Base64 인코딩된 암호화 바이너리>",
  "filename": "processed_demo_v1.npz.enc",
  "sha256": "abc123...",
  "size": 1048576,
  "artifactVersion": "v1",
  "encAlgo": "AES-256-GCM",
  "nonce": "...",
  "dekWrappedByKek": "...",
  "kekVersion": "v1"
}
```

---

## 5. 변경 영향 없는 API

아래 API는 변경 없이 기존과 동일하게 동작합니다.

| 엔드포인트 | 상태 |
|------------|------|
| `POST /api/teams/{teamId}/tasks` | 변경 없음 (업로드 인터페이스 동일) |
| `POST /api/tasks` | 변경 없음 |
| `GET /api/teams/{teamId}/tasks` | 변경 없음 |
| `POST /api/teams/{teamId}/demos` | 변경 없음 (업로드 인터페이스 동일) |
| `DELETE /api/teams/{teamId}/demos/{demoIndex}` | 변경 없음 |
| `GET /api/sync/tasks` | 변경 없음 |
| `POST /api/security/handshake` | 변경 없음 |

---

## 6. 클라이언트 수정 우선순위

| 우선순위 | 항목 | 영향 |
|---------|------|------|
| 🔴 필수 | `GET /api/tasks/{id}/data` 응답에서 `content` → `objectKey` 처리 | 미수정 시 다운로드 불가 |
| 🔴 필수 | `GET /api/sync/demos` 응답에서 `encryptedBlob` → `objectKey` 처리 | 미수정 시 sync 불가 |
| 🔴 필수 | S3에서 직접 다운로드하는 로직 추가 (boto3 등) | 위 두 항목과 연동 |
| 🟡 선택 | Scenario/Artifact API 연동 | 메타데이터 파이프라인 필요 시 |
