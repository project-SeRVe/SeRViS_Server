/**
 * Stress Test — 한계 탐색 (VU 10 → 200명 점진적 증가)
 * 실행: k6 run stress.js
 * 목적: 어느 시점에 에러/지연이 발생하는지, HPA 스케일아웃이 되는지 확인
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://k8s-servealb-a05f190fd7-1682512394.ap-northeast-2.elb.amazonaws.com';
const TEST_EMAIL    = __ENV.TEST_EMAIL    || 'loadtest@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'TestPass1234';
const TEST_TEAM_ID  = __ENV.TEST_TEAM_ID  || '';

const errorCount = new Counter('error_count');
const errorRate  = new Rate('error_rate');

export const options = {
  stages: [
    { duration: '1m',  target: 10  },  // 기준선
    { duration: '2m',  target: 50  },  // 증가
    { duration: '2m',  target: 100 },  // 고부하
    { duration: '2m',  target: 150 },  // 스트레스
    { duration: '2m',  target: 200 },  // 한계 탐색
    { duration: '2m',  target: 200 },  // 유지
    { duration: '1m',  target: 0   },  // 쿨다운
  ],
  thresholds: {
    // 스트레스 테스트는 임계값을 느슨하게 — 언제 깨지는지 관찰
    http_req_failed:   ['rate<0.10'],   // 에러율 10% 미만
    http_req_duration: ['p(95)<3000'],  // p95 3초 미만
  },
};

function login() {
  const res = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  const ok = res.status === 200;
  errorRate.add(!ok);
  if (!ok) errorCount.add(1);
  return ok ? res.json('accessToken') : null;
}

export default function () {
  const token = login();
  if (!token) { sleep(1); return; }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // 팀 목록 조회
  const teamsRes = http.get(`${BASE_URL}/api/repositories`, { headers });
  const teamsOk = check(teamsRes, { 'teams 200': (r) => r.status === 200 });
  if (!teamsOk) { errorRate.add(1); errorCount.add(1); sleep(1); return; }

  const teams = teamsRes.json();
  if (!teams || teams.length === 0) return;
  const teamId = TEST_TEAM_ID || teams[0].id;

  // Task 업로드
  const content = encoding.b64encode('stress-test-' + __VU + '-' + Date.now());
  const uploadRes = http.post(
    `${BASE_URL}/api/teams/${teamId}/tasks`,
    JSON.stringify({
      fileName: `stress_${__VU}_${Date.now()}.bin`,
      fileType: 'bin',
      encryptedBlob: content,
    }),
    { headers }
  );
  const uploadOk = check(uploadRes, { 'upload 200': (r) => r.status === 200 });
  if (!uploadOk) { errorRate.add(1); errorCount.add(1); }

  // Task 목록 + 다운로드
  const tasksRes = http.get(`${BASE_URL}/api/teams/${teamId}/tasks`, { headers });
  check(tasksRes, { 'tasks 200': (r) => r.status === 200 });

  const tasks = tasksRes.json();
  if (tasks && tasks.length > 0) {
    const downloadRes = http.get(`${BASE_URL}/api/tasks/${tasks[0].id}/data`, { headers });
    const dlOk = check(downloadRes, { 'download 200': (r) => r.status === 200 });
    if (!dlOk) { errorRate.add(1); errorCount.add(1); }
  }

  sleep(1);
}
