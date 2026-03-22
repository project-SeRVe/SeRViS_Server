/**
 * 부하테스트 사전 준비 스크립트
 * 실행: node setup.js 또는 k6 run setup.js
 *
 * 테스트 계정 생성 + 팀 생성 → 환경변수로 출력
 * 테스트 전 1회만 실행하면 됨
 */
import http from 'k6/http';
import { check } from 'k6';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://k8s-servealb-a05f190fd7-1682512394.ap-northeast-2.elb.amazonaws.com';

export const options = { vus: 1, iterations: 1 };

export default function () {
  const email    = 'loadtest@test.com';
  const password = 'TestPass1234';
  const pubKey   = encoding.b64encode('load-test-public-key');
  const encKey   = encoding.b64encode('load-test-encrypted-private-key');
  const teamKey  = encoding.b64encode('load-test-team-key');

  // 1. 회원가입
  const signupRes = http.post(`${BASE_URL}/auth/signup`,
    JSON.stringify({ email, password, username: 'loadtestuser', publicKey: pubKey, encryptedPrivateKey: encKey }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  console.log(`[signup] ${signupRes.status} — ${signupRes.body}`);

  // 2. 로그인
  const loginRes = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(loginRes, { 'login ok': (r) => r.status === 200 });
  const token = loginRes.json('accessToken');
  console.log(`[login] token: ${token ? token.substring(0, 30) + '...' : 'FAILED'}`);

  // 3. 팀 생성
  const teamRes = http.post(`${BASE_URL}/api/repositories`,
    JSON.stringify({ name: 'load-test-team', description: '부하테스트용 팀', encryptedTeamKey: teamKey }),
    { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
  );
  check(teamRes, { 'team created': (r) => r.status === 200 });
  const teamId = teamRes.json('id');

  console.log('\n========================================');
  console.log('부하테스트 환경변수 설정:');
  console.log(`export TEST_EMAIL="${email}"`);
  console.log(`export TEST_PASSWORD="${password}"`);
  console.log(`export TEST_TEAM_ID="${teamId}"`);
  console.log(`export BASE_URL="${BASE_URL}"`);
  console.log('========================================\n');
}
