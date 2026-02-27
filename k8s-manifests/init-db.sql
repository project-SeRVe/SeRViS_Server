-- ============================================================
-- RDS 초기화 SQL
-- Terraform으로 RDS 생성 후 실행
-- 접속 방법: socat proxy Pod + kubectl port-forward
-- ============================================================

-- DB 생성 (serve_auth_db는 RDS 생성 시 자동 생성됨)
CREATE DATABASE IF NOT EXISTS serve_team_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS serve_core_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 앱 사용자 생성 + 권한 부여
CREATE USER IF NOT EXISTS 'serve_user'@'%' IDENTIFIED BY 'tg8684ih';
GRANT ALL PRIVILEGES ON serve_auth_db.* TO 'serve_user'@'%';
GRANT ALL PRIVILEGES ON serve_team_db.* TO 'serve_user'@'%';
GRANT ALL PRIVILEGES ON serve_core_db.* TO 'serve_user'@'%';
FLUSH PRIVILEGES;
