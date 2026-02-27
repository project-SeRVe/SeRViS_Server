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
