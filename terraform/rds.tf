# ============================================================
# RDS MariaDB
# ⚠️ 콘솔에서 겪은 문제:
#   1. SSL 강제(require_secure_transport=ON) → 앱 연결 거부
#      → 파라미터 그룹에서 SSL 강제 해제 (테스트 환경)
#   2. 반복 연결 실패로 Host blocked → RDS Reboot 필요했음
#      → 파라미터 그룹에서 max_connect_errors 증가
#   3. Too many connections → db.t3.micro 커넥션 부족
#      → max_connections 증가
# ============================================================

# --- DB Subnet Group ---
resource "aws_db_subnet_group" "this" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

# --- DB Parameter Group ---
resource "aws_db_parameter_group" "this" {
  name   = "${var.project_name}-mariadb-params"
  family = "mariadb10.11"

  # ⚠️ SSL 강제 해제 (테스트 환경)
  # 프로덕션에서는 ON으로 변경하고 앱에서 SSL 인증서 지정
  parameter {
    name  = "require_secure_transport"
    value = "0"
  }

  # ⚠️ Host blocked 방지 — 연결 실패 허용 횟수 증가
  parameter {
    name  = "max_connect_errors"
    value = "999999"
  }

  # ⚠️ Too many connections 방지
  parameter {
    name  = "max_connections"
    value = "100"
  }

  # 한글 지원
  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }

  parameter {
    name  = "collation_server"
    value = "utf8mb4_general_ci"
  }

  tags = {
    Name = "${var.project_name}-mariadb-params"
  }
}

# --- RDS Instance ---
resource "aws_db_instance" "this" {
  identifier = "${var.project_name}-mariadb"

  engine         = "mariadb"
  engine_version = "10.11"
  instance_class = var.db_instance_class

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"

  db_name  = "serve_auth_db"
  username = var.db_master_username
  password = var.db_master_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = aws_db_parameter_group.this.name

  publicly_accessible = false
  multi_az            = false
  skip_final_snapshot = true

  tags = {
    Name = "${var.project_name}-mariadb"
  }
}
