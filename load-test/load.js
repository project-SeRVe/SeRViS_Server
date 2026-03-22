/**
 * Load Test — 일반 부하 (VU 50명, 5분)
 * 실행: k6 run load.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://k8s-servealb-a05f190fd7-1682512394.ap-northeast-2.elb.amazonaws.com';
const TEST_EMAIL    = __ENV.TEST_EMAIL    || 'loadtest@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'TestPass1234';
const TEST_TEAM_ID  = __ENV.TEST_TEAM_ID  || '';

// 커스텀 메트릭
const uploadDuration   = new Trend('upload_duration');
const downloadDuration = new Trend('download_duration');
const errorCount       = new Counter('error_count');

export const options = {
  stages: [
    { duration: '1m',  target: 20 },   // 워밍업: 1분 동안 20명까지
    { duration: '3m',  target: 50 },   // 부하: 3분 동안 50명 유지
    { duration: '1m',  target: 0  },   // 쿨다운
  ],
  thresholds: {
    http_req_failed:   ['rate<0.05'],   // 에러율 5% 미만 (S3 포함 현실적 기준)
    http_req_duration: ['p(95)<3000'],  // p95 3s 미만
    upload_duration:   ['p(95)<5000'],  // 업로드 p95 5s 미만 (S3 포함)
    download_duration: ['p(95)<3000'],  // 다운로드 p95 3s 미만
  },
};

function login() {
  const res = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  if (res.status !== 200) {
    errorCount.add(1);
    return null;
  }
  return res.json('accessToken');
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
  check(teamsRes, { 'teams 200': (r) => r.status === 200 });
  if (teamsRes.status !== 200) { errorCount.add(1); return; }

  const teams = teamsRes.json();
  if (!teams || teams.length === 0) return;
  const teamId = TEST_TEAM_ID || teams[0].id;

  // Task 업로드 (S3 + Feign 통신 포함)
  const content = encoding.b64encode('load-test-payload-' + __VU + '-' + Date.now());
  const uploadStart = Date.now();
  const uploadRes = http.post(
    `${BASE_URL}/api/teams/${teamId}/tasks`,
    JSON.stringify({
      fileName: `load_${__VU}_${Date.now()}.bin`,
      fileType: 'bin',
      encryptedBlob: content,
    }),
    { headers }
  );
  uploadDuration.add(Date.now() - uploadStart);
  check(uploadRes, { 'upload 200': (r) => r.status === 200 });
  if (uploadRes.status !== 200) errorCount.add(1);

  // Task 목록 조회
  const tasksRes = http.get(`${BASE_URL}/api/teams/${teamId}/tasks`, { headers });
  check(tasksRes, { 'tasks list 200': (r) => r.status === 200 });

  const tasks = tasksRes.json();
  if (tasks && tasks.length > 0) {
    // Task 다운로드 (objectKey 반환)
    const taskId = tasks[0].id;
    const downloadStart = Date.now();
    const downloadRes = http.get(`${BASE_URL}/api/tasks/${taskId}/data`, { headers });
    downloadDuration.add(Date.now() - downloadStart);
    check(downloadRes, { 'download 200': (r) => r.status === 200 });
    if (downloadRes.status !== 200) errorCount.add(1);
  }

  // Scenario 조회 (읽기 부하)
  const scenarioRes = http.get(`${BASE_URL}/api/scenarios`, { headers });
  check(scenarioRes, { 'scenarios 200': (r) => r.status === 200 });

  sleep(1);
}
