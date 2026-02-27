# ============================================================
# 보안 그룹
# ⚠️ 콘솔에서 겪은 문제:
#   1. 기본 VPC에 보안 그룹을 생성해서 상호 참조 불가
#      → 모든 보안 그룹에 vpc_id 명시
#   2. EKS 자율 모드 노드가 serve-eks-node-sg를 안 써서 RDS 접근 불가
#      → RDS SG에 VPC CIDR 전체(10.0.0.0/16) 허용
# ============================================================

# --- ALB Security Group ---
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb-sg"
  description = "Security group for ALB"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-alb-sg"
  }
}

# --- EKS Node Security Group ---
resource "aws_security_group" "eks_node" {
  name        = "${var.project_name}-eks-node-sg"
  description = "Security group for EKS worker nodes"
  vpc_id      = aws_vpc.this.id

  # ALB → 노드 (서비스 포트)
  ingress {
    description     = "ALB to service ports"
    from_port       = 8081
    to_port         = 8083
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # 노드 간 통신 (self)
  ingress {
    description = "Node to node all traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-eks-node-sg"
  }
}

# --- RDS Security Group ---
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Security group for RDS MariaDB"
  vpc_id      = aws_vpc.this.id

  # ⚠️ VPC CIDR 전체를 허용
  # 콘솔에서 EKS 자율 모드 노드가 eks-node-sg를 안 써서 접근 불가했음
  # VPC CIDR로 열면 어떤 노드/Pod에서든 접근 가능
  ingress {
    description = "MariaDB from VPC"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-rds-sg"
  }
}
