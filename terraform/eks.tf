# ============================================================
# EKS Cluster + Managed Node Group
# ⚠️ 콘솔에서 겪은 문제:
#   1. K8s 1.35 선택 시 Auto Mode 강제 활성화 → Managed Node Group과 충돌
#      → 1.31 사용 + Managed Node Group으로 명시적 생성
#   2. kubectl 인증 실패 — 액세스 항목 미설정
#      → aws_eks_access_entry로 자동 설정
# ============================================================

# --- EKS Cluster ---
resource "aws_eks_cluster" "this" {
  name     = var.cluster_name
  version  = var.kubernetes_version
  role_arn = aws_iam_role.eks_cluster.arn

  vpc_config {
    subnet_ids              = concat(aws_subnet.public[*].id, aws_subnet.private[*].id)
    endpoint_private_access = true
    endpoint_public_access  = true
    security_group_ids      = [aws_security_group.eks_node.id]
  }

  access_config {
    authentication_mode = "API_AND_CONFIG_MAP"
  }

  # Auto Mode 비활성화 — Managed Node Group 사용
  # K8s 1.35에서 Auto Mode가 강제 활성화되어 문제 발생했음

  depends_on = [
    aws_iam_role_policy_attachment.eks_cluster_policy,
    aws_iam_role_policy_attachment.eks_vpc_resource_controller,
  ]

  tags = {
    Name = var.cluster_name
  }
}

# --- EKS Addon: VPC CNI ---
resource "aws_eks_addon" "vpc_cni" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "vpc-cni"

  depends_on = [aws_eks_node_group.this]
}

# --- EKS Addon: CoreDNS ---
resource "aws_eks_addon" "coredns" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "coredns"

  depends_on = [aws_eks_node_group.this]
}

# --- EKS Addon: kube-proxy ---
resource "aws_eks_addon" "kube_proxy" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "kube-proxy"

  depends_on = [aws_eks_node_group.this]
}

# --- Managed Node Group ---
resource "aws_eks_node_group" "this" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "${var.project_name}-node-group"
  node_role_arn   = aws_iam_role.eks_node.arn
  subnet_ids      = aws_subnet.private[*].id

  instance_types = var.node_instance_types
  ami_type       = "AL2023_x86_64_STANDARD"
  # ⚠️ 콘솔에서는 Apple Silicon 맥북의 아키텍처와 관계없이 x86 선택해야 함

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  update_config {
    max_unavailable = 1
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_worker_node_policy,
    aws_iam_role_policy_attachment.eks_cni_policy,
    aws_iam_role_policy_attachment.ecr_read_only,
  ]

  tags = {
    Name = "${var.project_name}-node"
  }
}

# --- EKS Access Entry ---
# ⚠️ 콘솔에서 겪은 문제: 클러스터 생성자와 kubectl 사용자가 달라서 인증 실패
#    → 현재 사용자(terraform 실행자)에게 자동으로 admin 권한 부여
data "aws_caller_identity" "current" {}

resource "aws_eks_access_entry" "admin" {
  cluster_name  = aws_eks_cluster.this.name
  principal_arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:user/seRViS-deployer"
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "admin" {
  cluster_name  = aws_eks_cluster.this.name
  principal_arn = aws_eks_access_entry.admin.principal_arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }
}
