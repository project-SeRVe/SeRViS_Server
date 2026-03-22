/**
 * Smoke Test — 정상 동작 확인 (소수 VU, 짧은 시간)
 * 실행: k6 run smoke.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://k8s-servealb-a05f190fd7-1682512394.ap-northeast-2.elb.amazonaws.com';

export const options = {
  stages: [
    { duration: '10s', target: 3 },
    { duration: '20s', target: 3 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed:   ['rate<0.01'],       // 에러율 1% 미만
    http_req_duration: ['p(95)<1000'],      // p95 응답시간 1초 미만
  },
};

// 테스트용 계정 (서버에 미리 생성되어 있어야 함)
const TEST_EMAIL    = __ENV.TEST_EMAIL    || 'loadtest@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'TestPass1234';
const TEST_TEAM_ID  = __ENV.TEST_TEAM_ID  || '';

function login() {
  const res = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'login 200': (r) => r.status === 200 });
  return res.json('accessToken');
}

export default function () {
  // 1. 로그인
  const token = login();
  if (!token) return;

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // 2. 팀 목록 조회
  const teamsRes = http.get(`${BASE_URL}/api/repositories`, { headers });
  check(teamsRes, { 'GET /api/repositories 200': (r) => r.status === 200 });

  const teams = teamsRes.json();
  if (!teams || teams.length === 0) return;
  const teamId = TEST_TEAM_ID || teams[0].id;

  // 3. Task 업로드
  const content = encoding.b64encode('smoke-test-content-' + Date.now());
  const uploadRes = http.post(
    `${BASE_URL}/api/teams/${teamId}/tasks`,
    JSON.stringify({ fileName: `smoke_${Date.now()}.bin`, fileType: 'bin', encryptedBlob: content }),
    { headers }
  );
  check(uploadRes, { 'POST /api/teams/{id}/tasks 200': (r) => r.status === 200 });

  // 4. Task 목록 조회
  const tasksRes = http.get(`${BASE_URL}/api/teams/${teamId}/tasks`, { headers });
  check(tasksRes, { 'GET /api/teams/{id}/tasks 200': (r) => r.status === 200 });

  const tasks = tasksRes.json();
  if (tasks && tasks.length > 0) {
    // 5. Task 다운로드
    const taskId = tasks[0].id;
    const downloadRes = http.get(`${BASE_URL}/api/tasks/${taskId}/data`, { headers });
    check(downloadRes, { 'GET /api/tasks/{id}/data 200': (r) => r.status === 200 });
  }

  // 6. Scenario 생성
  const scenarioRes = http.post(
    `${BASE_URL}/api/scenarios`,
    JSON.stringify({ promptText: `smoke test scenario ${Date.now()}` }),
    { headers }
  );
  check(scenarioRes, { 'POST /api/scenarios 200': (r) => r.status === 200 });

  sleep(1);
}
