# ============================================================
# Variables
# ============================================================

variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "프로젝트명 (리소스 네이밍에 사용)"
  type        = string
  default     = "servis"
}

variable "cluster_name" {
  description = "EKS 클러스터명"
  type        = string
  default     = "SeRViS-eks"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes 버전"
  type        = string
  default     = "1.31"
  # ⚠️ 1.35는 Auto Mode 강제 활성화 문제 있었음. 1.31 사용 권장
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "azs" {
  description = "사용할 가용영역"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2b"]
}

variable "public_subnet_cidrs" {
  description = "Public 서브넷 CIDR"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "Private 서브넷 CIDR"
  type        = list(string)
  default     = ["10.0.128.0/24", "10.0.129.0/24"]
}

variable "node_instance_types" {
  description = "EKS 노드 인스턴스 타입"
  type        = list(string)
  default     = ["t3.medium"]
}

variable "node_desired_size" {
  description = "노드 그룹 희망 수"
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "노드 그룹 최소 수"
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "노드 그룹 최대 수"
  type        = number
  default     = 3
}

# RDS
variable "db_instance_class" {
  description = "RDS 인스턴스 타입"
  type        = string
  default     = "db.t3.micro"
}

variable "db_master_username" {
  description = "RDS 마스터 사용자명"
  type        = string
  default     = "admin"
}

variable "db_master_password" {
  description = "RDS 마스터 비밀번호"
  type        = string
  sensitive   = true
}

variable "db_app_username" {
  description = "애플리케이션 DB 사용자명"
  type        = string
  default     = "serve_user"
}

variable "db_app_password" {
  description = "애플리케이션 DB 비밀번호"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT 시크릿 키"
  type        = string
  sensitive   = true
}

# 서비스 목록
variable "services" {
  description = "MSA 서비스 목록"
  type = map(object({
    port    = number
    db_name = string
  }))
  default = {
    auth = { port = 8081, db_name = "serve_auth_db" }
    team = { port = 8082, db_name = "serve_team_db" }
    core = { port = 8083, db_name = "serve_core_db" }
  }
}
