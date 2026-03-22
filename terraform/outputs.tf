
resource "aws_ssm_parameter" "rds_host" {
  name      = "/servis/rds-host"
  type      = "String"
  value     = aws_db_instance.this.address
  overwrite = true
  tags      = { Name = "${var.project_name}-rds-host" }
}

resource "aws_ssm_parameter" "public_subnets" {
  name      = "/servis/public-subnets"
  type      = "String"
  value     = join(",", aws_subnet.public[*].id)
  overwrite = true
  tags      = { Name = "${var.project_name}-public-subnets" }
}

resource "aws_ssm_parameter" "ecr_registry" {
  name      = "/servis/ecr-registry"
  type      = "String"
  value     = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com"
  overwrite = true
  tags      = { Name = "${var.project_name}-ecr-registry" }
}

resource "aws_ssm_parameter" "cluster_name" {
  name      = "/servis/cluster-name"
  type      = "String"
  value     = var.cluster_name
  overwrite = true
  tags      = { Name = "${var.project_name}-cluster-name" }
}

# ============================================================
# Outputs
# ============================================================

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.this.id
}

output "eks_cluster_name" {
  description = "EKS 클러스터명"
  value       = aws_eks_cluster.this.name
}

output "eks_cluster_endpoint" {
  description = "EKS API 엔드포인트"
  value       = aws_eks_cluster.this.endpoint
}

output "rds_endpoint" {
  description = "RDS 엔드포인트"
  value       = aws_db_instance.this.endpoint
}

output "rds_address" {
  description = "RDS 호스트명 (포트 제외)"
  value       = aws_db_instance.this.address
}

output "ecr_repository_urls" {
  description = "ECR 리포지토리 URL"
  value       = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}

output "kubeconfig_command" {
  description = "kubeconfig 설정 명령어"
  value       = "aws eks update-kubeconfig --name ${var.cluster_name} --region ${var.aws_region}"
}

output "s3_artifacts_bucket" {
  description = "serve-core 아티팩트 S3 버킷명"
  value       = aws_s3_bucket.servis_artifacts.bucket
}

output "ecr_login_command" {
  description = "ECR 로그인 명령어"
  value       = "aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com"
}

# --- 배포 후 필요한 정보 ---
output "deploy_info" {
  description = "K8s 매니페스트 배포 시 필요한 정보"
  value = {
    rds_host      = aws_db_instance.this.address
    ecr_registry  = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com"
    public_subnets = join(",", aws_subnet.public[*].id)
  }
}

output "acm_certificate_arn" {
  description = "ACM 인증서 ARN"
  value       = aws_acm_certificate_validation.this.certificate_arn
}

output "hosted_zone_id" {
  description = "Route 53 Hosted Zone ID"
  value       = aws_route53_zone.this.zone_id
}

output "hosted_zone_name_servers" {
  description = "Hosted Zone 네임서버 (도메인 등록 정보에 수동 입력 필요)"
  value       = aws_route53_zone.this.name_servers
}
